package com.example.api.repository;

import com.example.api.model.entity.Driver;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository<Driver, String> {

    /**
     * Flips {@code driving} from {@code from} to {@code to}, and only from {@code from}.
     *
     * <p>Returns the number of rows changed, which is the whole point. The previous version set the
     * flag unconditionally and returned nothing, so two orders approved at the same moment could
     * both read "free", both write "busy", and send one driver out twice. With the expected value
     * in the WHERE clause the second UPDATE changes nothing and the caller is told so.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Driver d set d.driving = :to where d.id = :id and d.driving = :from")
    int switchDriving(@Param("id") String id, @Param("from") boolean from, @Param("to") boolean to);

    List<Driver> findAllByDriving(boolean driving);
}
