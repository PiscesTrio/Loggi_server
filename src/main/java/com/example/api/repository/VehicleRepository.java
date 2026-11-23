package com.example.api.repository;

import com.example.api.model.entity.Vehicle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    /**
     * Flips {@code driving} from {@code from} to {@code to}, and only from {@code from}.
     *
     * <p>Returns the number of rows changed, which is the whole point. The previous version set the
     * flag unconditionally and returned nothing, so two orders approved at the same moment could
     * both read "free", both write "busy", and send one vehicle out twice. With the expected value
     * in the WHERE clause the second UPDATE changes nothing and the caller is told so.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Vehicle v set v.driving = :to where v.id = :id and v.driving = :from")
    int switchDriving(@Param("id") String id, @Param("from") boolean from, @Param("to") boolean to);

    List<Vehicle> findAllByDriving(boolean driving);
}
