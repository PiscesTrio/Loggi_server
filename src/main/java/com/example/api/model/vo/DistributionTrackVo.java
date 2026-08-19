package com.example.api.model.vo;

import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.DistributionTrack;
import com.example.api.model.enums.DistributionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * One point on a delivery's trail.
 *
 * <p>The parent is a plain {@code distributionId}, which is what it always was on the wire -
 * the column used to be a bare string called {@code disId}. S09 turned it into an association
 * and kept the id shape with {@code @JsonIdentityReference}, which writes an id but cannot
 * read a lone one back, so the response carried an id while a request had to send an object.
 * A view type has no such constraint: the field is a String because that is what belongs
 * here, in both directions.
 */
public record DistributionTrackVo(
        String id,
        String distributionId,
        double lat,
        double lng,
        String location,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime time,
        DistributionStatus status) {

    public static DistributionTrackVo from(DistributionTrack track) {
        Distribution parent = track.getDistribution();
        return new DistributionTrackVo(
                track.getId(),
                parent == null ? null : parent.getId(),
                track.getLat(),
                track.getLng(),
                track.getLocation(),
                track.getTime(),
                track.getStatus());
    }
}
