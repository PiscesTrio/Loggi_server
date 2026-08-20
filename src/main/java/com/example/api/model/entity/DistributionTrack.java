package com.example.api.model.entity;

import com.example.api.model.enums.DistributionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

/**
 * One point on a delivery order's trail: where the vehicle was, when, and which stage of the order
 * that sighting belongs to. Rows accumulate per order and the app draws them as a timeline.
 *
 * <p>This class was called {@code DistributionStatus} — the same simple name as {@link
 * com.example.api.model.enums.DistributionStatus}, which is the order's state machine (REVIEWING /
 * REVIEW_SUCCESS / END). Different packages, so it compiled, but a reader seeing {@code
 * DistributionStatus} in a signature could not tell which of the two was meant without checking the
 * imports, and any file needing both could only have one unqualified. A track point is not a
 * status, so it is named for what it is.
 *
 * <p>The {@code status} field below is the one genuinely status-shaped thing here: it is the
 * order's stage at the moment this point was recorded.
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
     * The order this sighting belongs to. Was {@code disId}, a string with no foreign key — so a
     * track point could outlive the order it described, and the timeline query that reads them by
     * that column had no way to know.
     */
    // The @JsonIdentityReference that used to sit here is gone with S10's view types. It
    // existed to make an association serialise as a bare id while the entity was itself the
    // wire format - a workaround for a problem the boundary removed, and it could only ever
    // work in one direction: it wrote an id and could not read a lone one back.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distribution_id")
    private Distribution distribution;

    // latitude
    private double lat;
    // longitude
    private double lng;

    private String location;

    private LocalDateTime time;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DistributionStatus status;
}
