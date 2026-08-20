package com.example.api.service.impl;

import com.example.api.model.dto.SystemLogQuery;
import com.example.api.model.entity.SystemLog;
import com.example.api.repository.SystemLogRepository;
import com.example.api.service.SystemLogService;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemLogServiceImpl implements SystemLogService {
    @Autowired private SystemLogRepository systemLogRepository;

    @Override
    public void record(SystemLog log) {
        systemLogRepository.save(log);
    }

    /**
     * One page of the log, newest first.
     *
     * <p>It returned every row. That is fine on a demo database with a few dozen and indefensible
     * on anything that has been running: this table grows by one row per audited request, forever,
     * and the endpoint would eventually load every one of them into memory to serialise them into a
     * response no client can use. Ordering is part of the contract here, because "the first page"
     * is meaningless without it.
     */
    @Override
    public Page<SystemLog> getAll(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }

    @Override
    public void delete(String id) {
        systemLogRepository.deleteById(id);
    }

    /**
     * The operation log, filtered by whichever fields the caller supplied.
     *
     * <p>Rewritten from a nested if/else that enumerated the combinations: account only, module
     * only, both, neither. Two optional filters is four branches, and each one built the same
     * predicate again - a third filter would have been eight. Collecting the predicates that apply
     * and joining them says the same thing once.
     *
     * <p>Both are substring matches, as they were. A leading wildcard cannot use an index, which is
     * why S08 deliberately did not create one for these columns.
     */
    @Override
    public Page<SystemLog> query(SystemLogQuery filter, Pageable pageable) {
        Specification<SystemLog> specification =
                (root, criteriaQuery, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    if (StringUtils.hasText(filter.getAccount())) {
                        predicates.add(
                                cb.like(root.get("account"), "%" + filter.getAccount() + "%"));
                    }
                    if (StringUtils.hasText(filter.getModule())) {
                        predicates.add(cb.like(root.get("module"), "%" + filter.getModule() + "%"));
                    }
                    // An empty list means no restriction, which is the honest reading of a request
                    // that named nothing to filter on. The old version returned null here, which
                    // Spring Data also treats as "no restriction" - by accident rather than by
                    // saying so.
                    return cb.and(predicates.toArray(new Predicate[0]));
                };
        return systemLogRepository.findAll(specification, pageable);
    }
}
