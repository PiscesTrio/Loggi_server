package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.Driver;
import com.example.api.model.entity.Vehicle;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.DriverRepository;
import com.example.api.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Approving an order takes a driver and a truck; completing it gives them back.
 *
 * <p>The driver/vehicle validation existed in this service as four commented-out lines. It
 * was not restored where it was found: it sat in the <em>completion</em> branch and read
 * "if the driver is driving, refuse", and a driver completing a delivery is driving by
 * definition, having been marked so on approval. Uncommenting it would have rejected every
 * completion the system can produce — which is the likeliest reason it was commented out.
 *
 * <p>So the two halves went where each is true: existence is checked on both transitions,
 * availability only on approval. The first two tests below are what keeps them there.
 */
@ExtendWith(MockitoExtension.class)
class DistributionServiceImplTest {

    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_COMPLETED = 2;

    @Mock DistributionRepository distributionRepository;
    @Mock DriverRepository driverRepository;
    @Mock VehicleRepository vehicleRepository;
    @InjectMocks DistributionServiceImpl service;

    private Distribution order(int status) {
        Distribution d = new Distribution();
        d.setDid("d1");
        d.setVid("v1");
        d.setStatus(status);
        return d;
    }

    private Driver driver(boolean driving) {
        Driver d = new Driver();
        d.setDriving(driving);
        return d;
    }

    private Vehicle vehicle(boolean driving) {
        Vehicle v = new Vehicle();
        v.setDriving(driving);
        return v;
    }

    @Test
    @DisplayName("Approval marks the driver and vehicle as driving")
    void approve_marksBothAsDriving() throws Exception {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver(false)));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle(false)));
        when(distributionRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        Distribution saved = service.save(order(STATUS_APPROVED));

        assertThat(saved.getStatus()).isEqualTo(STATUS_APPROVED);
        verify(driverRepository).updateDriving(true, "d1");
        verify(vehicleRepository).updateDriving(true, "v1");
    }

    @Test
    @DisplayName("Completion releases a driver who is - necessarily - currently driving")
    void complete_releasesADrivingDriver() throws Exception {
        // The exact case the commented-out check would have rejected. Both are driving,
        // because approval marked them so, and completion must still go through.
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver(true)));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle(true)));
        when(distributionRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        service.save(order(STATUS_COMPLETED));

        verify(driverRepository).updateDriving(false, "d1");
        verify(vehicleRepository).updateDriving(false, "v1");
    }

    @Test
    @DisplayName("Approval refuses a driver who is already out on another delivery")
    void approve_whenDriverBusy_throws409AndAssignsNothing() {
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver(true)));
        // Both lookups happen before either availability check, so the truck has to exist
        // for this test to be about the driver.
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle(false)));

        assertThatThrownBy(() -> service.save(order(STATUS_APPROVED)))
                .isInstanceOf(BizException.class)
                .hasMessage("司机当前不可用")
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(409);

        verify(driverRepository, never()).updateDriving(any(Boolean.class), any());
        verifyNoInteractions(distributionRepository);
    }

    @Test
    @DisplayName("Approval refuses a truck that is already out, without having taken the driver")
    void approve_whenVehicleBusy_leavesTheDriverFree() {
        // The order of the two writes is why this matters. updateDriving on the driver used
        // to run before anything looked at the vehicle, and without a transaction it
        // committed - so a rejected order still left a driver marked busy for a trip that
        // was never recorded, and /api/distribution/can hid them from every later dispatch.
        when(driverRepository.findById("d1")).thenReturn(Optional.of(driver(false)));
        when(vehicleRepository.findById("v1")).thenReturn(Optional.of(vehicle(true)));

        assertThatThrownBy(() -> service.save(order(STATUS_APPROVED)))
                .isInstanceOf(BizException.class)
                .hasMessage("货车当前不可用");

        verify(driverRepository, never()).updateDriving(any(Boolean.class), any());
        verifyNoInteractions(distributionRepository);
    }

    @Test
    @DisplayName("An id that matches no driver is refused, not written through")
    void approve_whenDriverIdUnknown_throws404() {
        // updateDriving is a bulk UPDATE. A wrong id matched no rows and reported success,
        // so the order was saved pointing at a driver who does not exist.
        when(driverRepository.findById("d1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(order(STATUS_APPROVED)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在的司机id")
                .extracting(e -> ((BizException) e).getStatus()).isEqualTo(404);

        verifyNoInteractions(distributionRepository);
    }

    @Test
    @DisplayName("A newly created order touches neither the driver nor the vehicle")
    void create_underReview_assignsNobody() throws Exception {
        // Status 0 is REVIEWING: the order exists, nothing is dispatched yet.
        when(distributionRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        service.save(order(0));

        verifyNoInteractions(driverRepository, vehicleRepository);
    }

    @Test
    @DisplayName("A null status is saved rather than throwing a NullPointerException")
    void save_withNullStatus_doesNotUnboxNull() throws Exception {
        // `distribution.getStatus() == 2` unboxed an Integer. The client always sends a
        // status, so this never fired in practice - which is exactly the kind of NPE that
        // waits for the one caller who does not.
        Distribution d = order(0);
        d.setStatus(null);
        when(distributionRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        assertThat(service.save(d)).isSameAs(d);
        verifyNoInteractions(driverRepository, vehicleRepository);
    }
}
