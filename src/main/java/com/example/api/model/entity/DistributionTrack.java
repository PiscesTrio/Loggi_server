package com.example.api.model.entity;


import com.example.api.model.enums.DistributionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "distribution")
@Entity
@NoArgsConstructor
public class DistributionTrack {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    /**
     * The order this sighting belongs to. Was {@code disId}, a string with no foreign key —
     * so a track point could outlive the order it described, and the timeline query that
     * reads them by that column had no way to know.
     */
    // Serialised as the id alone, not as a nested object. These columns were plain id
    // strings before this slice (disId, wid, cid), so emitting an id keeps the client seeing
    // what it saw; inlining the row instead would nest a Distribution — with its own three
    // associations — inside every track point. Distribution's own driver/vehicle/warehouse
    // are treated the other way for the same reason: the client used to receive a copied
    // name and plate there, so it gets the real rows.
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_id")
    private Distribution distribution;

    //latitude
    private double lat;
    //longitude
    private  double lng;

    private  String location;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime time;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DistributionStatus status;
}
