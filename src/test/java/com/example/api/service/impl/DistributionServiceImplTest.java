package com.example.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.model.enums.DistributionStatus;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * An order is filed under review, approved, then completed - and nothing else.
 *
 * <p>Approving takes the order's driver and truck; completing gives them back. Both used to happen
 * inside the one save method the API had, keyed on whatever status the request carried, so filing
 * an order as END released a driver who might be out on another delivery, and re-posting an order
 * to approve it filed a copy. These pin the split: {@code create} only files, {@code changeStatus}
 * only moves, and every move is checked against the state machine on {@link DistributionStatus}.
 *
 * <p>What a mock cannot show is two of these racing; that is DistributionStatusIT's job.
 */
@ExtendWith(MockitoExtension.class)
class DistributionServiceImplTest {

    @Mock DistributionRepository distributionRepository;
    @Mock DriverRepository driverRepository;
    @Mock VehicleRepository vehicleRepository;
    @InjectMocks DistributionServiceImpl service;

    /** An order naming a driver and a vehicle by id, which is all a request carries. */
    private static Distribution order(DistributionStatus status) {
        Distribution d = new Distribution();
        d.setId("o1");
        Driver driver = new Driver();
        driver.setId("d1");
        Vehicle vehicle = new Vehicle();
        vehicle.setId("v1");
        d.setDriver(driver);
        d.setVehicle(vehicle);
        d.setStatus(status);
        return d;
    }

    /** The order as the database currently holds it. */
    private void stored(DistributionStatus status) {
        when(distributionRepository.findById("o1")).thenReturn(Optional.of(order(status)));
    }

    private static Driver driver() {
        Driver d = new Driver();
        d.setId("d1");
        return d;
    }

    private static Vehicle vehicle() {
        Vehicle v = new Vehicle();
        v.setId("v1");
        return v;
    }

    private static ErrorCode codeOf(Throwable e) {
        return ((BizException) e).getErrorCode();
    }

    // --- create --------------------------------------------------------------------------

    @Test
    @DisplayName("A new order is filed under review, dispatches nobody, but checks who it names")
    void create_underReview_validatesButAssignsNobody() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver()));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle()));
        when(distributionRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        service.create(order(DistributionStatus.REVIEWING));

        verify(driverRepository, never()).switchDriving(anyString(), anyBoolean(), anyBoolean());
        verify(vehicleRepository, never()).switchDriving(anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("A new order cannot be filed as already finished, and so cannot free anyone")
    void create_asEnd_isRefusedAndReleasesNobody() {
        // The hole this closes: filing an order as END used to release whichever driver and
        // vehicle it named, whether or not they were out on another delivery.
        assertThatThrownBy(() -> service.create(order(DistributionStatus.END)))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        verifyNoInteractions(driverRepository, vehicleRepository, distributionRepository);
    }

    @Test
    @DisplayName("A new order naming a driver that does not exist is a 404, not a write")
    void create_whenDriverIdUnknown_throws404() {
        when(driverRepository.findById("d1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(order(DistributionStatus.REVIEWING)))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.DRIVER_NOT_FOUND);
        verifyNoInteractions(distributionRepository);
    }

    // --- changeStatus --------------------------------------------------------------------

    @Test
    @DisplayName("Approval moves the same order and takes its driver and vehicle")
    void approve_movesTheOrderAndTakesBoth() {
        stored(DistributionStatus.REVIEWING);
        when(distributionRepository.moveStatus(
                        eq("o1"),
                        eq(DistributionStatus.REVIEWING),
                        eq(DistributionStatus.REVIEW_SUCCESS),
                        any()))
                .thenReturn(1);
        when(driverRepository.switchDriving("d1", false, true)).thenReturn(1);
        when(vehicleRepository.switchDriving("v1", false, true)).thenReturn(1);
        when(distributionRepository.findWithAssociationsById("o1"))
                .thenReturn(Optional.of(order(DistributionStatus.REVIEW_SUCCESS)));

        Distribution result = service.changeStatus("o1", DistributionStatus.REVIEW_SUCCESS);

        // The same row moved, not a new one filed beside it.
        assertThat(result.getId()).isEqualTo("o1");
        assertThat(result.getStatus()).isEqualTo(DistributionStatus.REVIEW_SUCCESS);
        verify(distributionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Approval refuses a driver who is already out, and never reaches the vehicle")
    void approve_whenDriverBusy_throws409() {
        stored(DistributionStatus.REVIEWING);
        when(distributionRepository.moveStatus(any(), any(), any(), any())).thenReturn(1);
        when(driverRepository.switchDriving("d1", false, true)).thenReturn(0);

        assertThatThrownBy(() -> service.changeStatus("o1", DistributionStatus.REVIEW_SUCCESS))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.DRIVER_UNAVAILABLE);
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("Approval refuses a truck that is already out")
    void approve_whenVehicleBusy_throws409() {
        // The driver was taken a statement earlier; the throw is what makes the transaction
        // give them back. DistributionStatusIT checks that against a real database.
        stored(DistributionStatus.REVIEWING);
        when(distributionRepository.moveStatus(any(), any(), any(), any())).thenReturn(1);
        when(driverRepository.switchDriving("d1", false, true)).thenReturn(1);
        when(vehicleRepository.switchDriving("v1", false, true)).thenReturn(0);

        assertThatThrownBy(() -> service.changeStatus("o1", DistributionStatus.REVIEW_SUCCESS))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.VEHICLE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Completion releases the driver and vehicle the order took")
    void complete_releasesBoth() {
        stored(DistributionStatus.REVIEW_SUCCESS);
        when(distributionRepository.moveStatus(
                        eq("o1"),
                        eq(DistributionStatus.REVIEW_SUCCESS),
                        eq(DistributionStatus.END),
                        any()))
                .thenReturn(1);
        when(distributionRepository.findWithAssociationsById("o1"))
                .thenReturn(Optional.of(order(DistributionStatus.END)));

        service.changeStatus("o1", DistributionStatus.END);

        verify(driverRepository).switchDriving("d1", true, false);
        verify(vehicleRepository).switchDriving("v1", true, false);
    }

    @Test
    @DisplayName("An order under review cannot skip straight to finished")
    void complete_withoutApproval_isRefused() {
        stored(DistributionStatus.REVIEWING);

        assertThatThrownBy(() -> service.changeStatus("o1", DistributionStatus.END))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        verify(distributionRepository, never()).moveStatus(any(), any(), any(), any());
        verifyNoInteractions(driverRepository, vehicleRepository);
    }

    @Test
    @DisplayName("A finished order is final")
    void finishedOrder_cannotMove() {
        stored(DistributionStatus.END);

        for (DistributionStatus next : DistributionStatus.values()) {
            assertThatThrownBy(() -> service.changeStatus("o1", next))
                    .extracting(DistributionServiceImplTest::codeOf)
                    .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        verifyNoInteractions(driverRepository, vehicleRepository);
    }

    @Test
    @DisplayName("An order moved by someone else since it was read is refused, not moved twice")
    void approve_whenOrderMovedConcurrently_isRefused() {
        // The read said REVIEWING; by the time of the write another approval had moved it.
        // The conditional UPDATE changes no row, and the driver is never touched.
        stored(DistributionStatus.REVIEWING);
        when(distributionRepository.moveStatus(any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.changeStatus("o1", DistributionStatus.REVIEW_SUCCESS))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
        verifyNoInteractions(driverRepository, vehicleRepository);
    }

    @Test
    @DisplayName("Moving an order that does not exist is a 404")
    void changeStatus_whenOrderUnknown_throws404() {
        when(distributionRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus("ghost", DistributionStatus.END))
                .extracting(DistributionServiceImplTest::codeOf)
                .isEqualTo(ErrorCode.DISTRIBUTION_NOT_FOUND);
    }
}
