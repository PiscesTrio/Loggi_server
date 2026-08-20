package com.example.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * The filter a caller sends when searching the operation log.
 *
 * <p>It was called SystemLogVo and lived among the view types, which it never was: a VO describes
 * what an endpoint answers with, and this is what the caller asks with. The name mattered more than
 * usual here because the actual view type for this resource did not exist, so the request binder
 * was the only thing with the name.
 */
public class SystemLogQuery {
    private String account;
    private String module;
}
