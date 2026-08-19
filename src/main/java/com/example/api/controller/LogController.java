package com.example.api.controller;

import com.example.api.model.dto.SystemLogQuery;
import com.example.api.model.vo.LoginLogVo;
import com.example.api.model.vo.PageVo;
import com.example.api.model.vo.SystemLogVo;
import com.example.api.service.LoginLogService;
import com.example.api.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

/**
 * The two audit logs.
 *
 * <p>Both endpoints returned every row. That is fine against a demo database and
 * indefensible against one that has been running: these tables grow by a row per audited
 * request and a row per login attempt, without bound, and the endpoint would eventually
 * load all of them into memory to build a response no client can use.
 *
 * <p>They are the only two lists in this API that are paginated, and deliberately so. A
 * warehouse list is three rows; wrapping it in a page envelope would be ceremony that makes
 * every caller unwrap something to find what it already had. Pagination is here because
 * these two grow forever, not because lists should be paginated.
 */
@Tag(name = "Audit logs", description = "Who did what, and who tried to sign in. Both are paginated.")
@RestController
@RequestMapping("/api")
public class LogController {

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private SystemLogService systemLogService;

    @GetMapping("/loginlog")
    public PageVo<LoginLogVo> getLoginLog(
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageVo.of(loginLogService.getAll(pageable), LoginLogVo::from);
    }

    @DeleteMapping("/loginlog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delLoginLog(@PathVariable String id) {
        loginLogService.delLoginLog(id);
    }

    @GetMapping("/systemlog")
    public PageVo<SystemLogVo> getSystemLog(
            @PageableDefault(size = 20, sort = "time", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageVo.of(systemLogService.getAll(pageable), SystemLogVo::from);
    }

    @DeleteMapping("/systemlog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSystemLogById(@PathVariable String id) {
        systemLogService.delete(id);
    }

    /**
     * The same list, filtered.
     *
     * <p>Kept as its own path rather than folded into the one above, because the client
     * calls neither: /querySystemlog has never been called by the app at all. Merging them
     * would be a change to an endpoint nobody uses, made blind.
     */
    @GetMapping("/querySystemlog")
    public PageVo<SystemLogVo> querySystemlog(
            SystemLogQuery filter,
            @PageableDefault(size = 20, sort = "time", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageVo.of(systemLogService.query(filter, pageable), SystemLogVo::from);
    }
}
