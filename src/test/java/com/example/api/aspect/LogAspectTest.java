package com.example.api.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.api.annotation.Log;
import com.example.api.model.entity.SystemLog;
import com.example.api.model.enums.BusinessType;
import com.example.api.model.enums.LogModule;
import com.example.api.service.SystemLogService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The audit log records what happened; it must not become what happened.
 *
 * <p>The aspect wrote its record from a bare {@code finally} with no try/catch, so an exception
 * from the audit write propagated out of that block — replacing whatever the request was already
 * doing. A logging failure became the caller's failure, and if the request was itself failing, the
 * audit write erased the exception it existed to record.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogAspectTest {

    /** Stands in for a controller method; only its annotation and name are read. */
    static class Fixture {
        @Log(module = LogModule.COMMODITY, type = BusinessType.INSERT)
        public void annotated() {}
    }

    @Mock SystemLogService logService;
    @Mock ProceedingJoinPoint point;
    @Mock MethodSignature signature;

    private final LogAspect aspect = new LogAspect();

    @BeforeEach
    void wire() throws Exception {
        ReflectionTestUtils.setField(aspect, "logService", logService);
        Method method = Fixture.class.getMethod("annotated");
        when(point.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getDeclaringTypeName())
                .thenReturn("com.example.api.controller.WarehouseController");
        when(signature.getName()).thenReturn("save");
    }

    @Test
    @DisplayName("A successful call is recorded as successful, with its elapsed time")
    void success_isRecordedWithACost() throws Throwable {
        when(point.proceed()).thenReturn("result");

        assertThat(aspect.around(point)).isEqualTo("result");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(logService).record(captor.capture());
        SystemLog recorded = captor.getValue();
        assertThat(recorded.isSuccess()).isTrue();
        // Measured from the first version of this aspect and assigned to a local nobody
        // read, because SystemLog had no column to put it in.
        assertThat(recorded.getCostMs()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(recorded.getModule()).isEqualTo(LogModule.COMMODITY);
        // The enum, not its label. The aspect used to store annotation.type().getName() —
        // the Chinese display text — so the audit table held UI language and a reader had to
        // map it back. S10 dropped the toString() override for the same reason (Jackson 3
        // serialises through toString(), so the wire carried the label while the database
        // held the name); the label field itself is gone as of S20, because after S10 nothing
        // read it and the client maps the value to display text in its own locale.
        assertThat(recorded.getBusinessType()).isEqualTo(BusinessType.INSERT);
    }

    @Test
    @DisplayName("A failed call is recorded as failed, and its exception still reaches the caller")
    void failure_isRecordedAsFailureAndRethrown() throws Throwable {
        when(point.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.around(point))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(logService).record(captor.capture());
        // The record is still written - an operation that was attempted and failed is
        // exactly what an audit trail is for - but it no longer claims to have worked.
        assertThat(captor.getValue().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("An audit write that throws does not fail the request it was recording")
    void auditWriteFailure_doesNotSurfaceToTheCaller() throws Throwable {
        when(point.proceed()).thenReturn("result");
        doThrow(new RuntimeException("audit db down")).when(logService).record(any());

        // Previously this came out of the finally block and the caller saw "audit db down"
        // instead of their result.
        assertThat(aspect.around(point)).isEqualTo("result");
    }

    @Test
    @DisplayName("An audit write that throws does not swallow the request's own exception")
    void auditWriteFailure_doesNotReplaceTheRealException() throws Throwable {
        when(point.proceed()).thenThrow(new IllegalStateException("the real problem"));
        doThrow(new RuntimeException("audit db down")).when(logService).record(any());

        // The worst version of the old behaviour: the request failed, the audit write
        // failed on the way out, and the exception the caller received described the
        // logger rather than the fault - with the original discarded entirely.
        assertThatThrownBy(() -> aspect.around(point))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("the real problem");
    }

    @Test
    @DisplayName(
            "The stored method name drops the shared package prefix without counting characters")
    void methodName_stripsThePackagePrefix() throws Throwable {
        when(point.proceed()).thenReturn(null);

        aspect.around(point);

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(logService).record(captor.capture());
        assertThat(captor.getValue().getMethod()).isEqualTo("controller.WarehouseController.save");
    }

    @Test
    @DisplayName("A class outside the base package is stored whole rather than truncated")
    void methodName_outsideBasePackage_isNotTruncated() throws Throwable {
        // substring(16) was the length of "com.example.api." and nothing checked for it.
        // A shorter qualified name threw StringIndexOutOfBoundsException from inside the
        // audit logger, which - before the try/catch above - became the caller's error.
        when(signature.getDeclaringTypeName()).thenReturn("a.B");
        when(signature.getName()).thenReturn("c");
        when(point.proceed()).thenReturn(null);

        aspect.around(point);

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(logService).record(captor.capture());
        assertThat(captor.getValue().getMethod()).isEqualTo("a.B.c");
    }

    @Test
    @DisplayName("No HTTP request behind the call means a null IP, not a NullPointerException")
    void offRequestThread_recordsANullIp() throws Throwable {
        // RequestContextHolder is empty here, as it is for a scheduled task reaching an
        // annotated method. The old code called getRequest() on the null it returned.
        when(point.proceed()).thenReturn(null);

        aspect.around(point);

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(logService).record(captor.capture());
        assertThat(captor.getValue().getIp()).isNull();
    }
}
