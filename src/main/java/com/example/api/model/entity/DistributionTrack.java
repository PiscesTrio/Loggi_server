package com.example.api.model.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

/**
 * One point on a delivery order's trail: where the vehicle was, when, and which stage of
 * the order that sighting belongs to. Rows accumulate per order and the app draws them as
 * a timeline.
 *
 * <p>This class was called {@code DistributionStatus} — the same simple name as
 * {@link com.example.api.model.enums.DistributionStatus}, which is the order's state
 * machine (REVIEWING / REVIEW_SUCCESS / END). Different packages, so it compiled, but a
 * reader seeing {@code DistributionStatus} in a signature could not tell which of the two
 * was meant without checking the imports, and any file needing both could only have one
 * unqualified. A track point is not a status, so it is named for what it is.
 *
 * <p>The {@code status} field below is the one genuinely status-shaped thing here: it is
 * the order's stage at the moment this point was recorded.
 */
@Data
@Entity
@NoArgsConstructor
public class DistributionTrack {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String disId;

    //latitude
    private double lat;
    //longitude
    private  double lng;

    private  String location;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    private  Integer status;
}
