package com.example.api.model.entity;

import com.example.api.model.enums.CareTag;
import com.example.api.model.enums.DistributionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

/** Distribution */
@Data
@ToString(exclude = {"driver", "vehicle", "warehouse"})
// Identity is the id and nothing else: two rows with the same id are the same row,
// whatever their other columns say. callSuper = false because the superclass holds
// only timestamps, and when a row was last touched is not part of what it is.
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@NoArgsConstructor
public class Distribution extends Auditable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    /**
     * The driver, the vehicle and the warehouse this order starts from.
     *
     * <p>They were three strings - did, vid, wid - with no foreign key behind them, next to two
     * more strings holding the driver's name and the plate. That arrangement had three separate
     * problems. Reading the driver's name meant a second query by hand, which is where N+1 comes
     * from. The duplicated name and plate drifted the moment either was edited, and nothing
     * reconciled them. And nothing at all stopped an order referencing a driver who does not exist
     * - the column took any string.
     *
     * <p>LAZY on every one of them. {@code @ManyToOne} defaults to EAGER, which means every query
     * for an order silently joins three more tables whether or not the caller wanted them. With
     * {@code open-in-view: false} that makes the fetch boundary explicit rather than accidental:
     * repositories declare what they need with {@code @EntityGraph}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    // customer phone
    private String phone;

    // customer address
    private String address;

    // expedited handling
    private boolean urgent;

    /**
     * Handling instructions, one row each in {@code distribution_care}.
     *
     * <p>Was a single varchar holding the selected Chinese labels comma-joined with a trailing
     * comma, because that is the string the client's multi-select produced. The column therefore
     * stored a client's serialisation format, and the seed file needed a comment explaining that
     * the trailing comma was deliberate. Rows instead — the same move V7 made for admin.roles, for
     * the same reasons: a tag can be queried, an unknown tag cannot be stored, and "no tags" has
     * one representation rather than three.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "distribution_care",
            joinColumns = @JoinColumn(name = "distribution_id"),
            foreignKey = @ForeignKey(name = "fk_distribution_care_distribution"))
    @Column(name = "tag", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<CareTag> care = new LinkedHashSet<>();

    // operation time
    private LocalDateTime time;

    /**
     * Where the order is in its lifecycle.
     *
     * <p>Was an Integer holding 0, 1 or 2, while an enum named REVIEWING / REVIEW_SUCCESS / END sat
     * in the same codebase referenced from nowhere. Stored as the name rather than the ordinal: an
     * ordinal silently changes meaning the day someone inserts a constant in the middle, and
     * nothing in the database would show it happened.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DistributionStatus status;

    private double fromLat;

    private double fromLng;

    private double toLat;

    private double toLng;
}
