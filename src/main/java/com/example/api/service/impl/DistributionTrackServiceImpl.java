package com.example.api.service.impl;

import com.example.api.exception.BizException;
import com.example.api.exception.ErrorCode;
import com.example.api.model.entity.Distribution;
import com.example.api.model.entity.DistributionTrack;
import com.example.api.repository.DistributionRepository;
import com.example.api.repository.DistributionTrackRepository;
import com.example.api.service.DistributionTrackService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DistributionTrackServiceImpl implements DistributionTrackService {

    @Resource private DistributionTrackRepository distributionTrackRepository;

    @Resource private DistributionRepository distributionRepository;

    @Override
    public List<DistributionTrack> findByDisId(String disId) {
        return distributionTrackRepository.findAllByDistributionId(disId);
    }

    /**
     * Records a sighting.
     *
     * <p>The parent is resolved rather than written through. A request names an order by id and
     * nothing more, so without this the first thing to notice a wrong id is the foreign key - and a
     * constraint violation reaches the handler that answers "this already exists", which is not
     * what happened. Resolving here makes it a 404 naming the order.
     *
     * <p>The time is set here and not accepted from the caller. A tracking record is the server's
     * statement about where a vehicle was and when; letting the client supply the timestamp would
     * make it the client's statement instead, which is the one thing this record exists to be
     * independent of.
     */
    @Override
    public DistributionTrack save(DistributionTrack track) {
        String distributionId =
                track.getDistribution() == null ? null : track.getDistribution().getId();
        Distribution parent =
                distributionRepository
                        .findById(distributionId == null ? "" : distributionId)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.DISTRIBUTION_NOT_FOUND,
                                                "no delivery order with id " + distributionId));

        track.setDistribution(parent);
        track.setTime(LocalDateTime.now());
        return distributionTrackRepository.save(track);
    }
}
