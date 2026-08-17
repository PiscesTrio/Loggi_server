package com.example.api.security;

import com.example.api.model.support.ResponseResult;
import com.example.api.utils.JwtTokenUtil;
import com.example.api.utils.ResponseUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the JWT from the request's Authorization header and
 * parses it into the Spring Security authentication.
 */

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        //Pull the token out of the request header
        String token = request.getHeader(JwtTokenUtil.TOKEN_HEADER);

        //No token: let the request through
        //Spring Security returns 403 later if the target URL is not public
        if (!JwtTokenUtil.checkToken(token)){
            chain.doFilter(request, response);
            return;
        }

        //Reject expired JWTs
        if (JwtTokenUtil.isExpiration(token)) {
            ResponseUtil.writeJson(response, new ResponseResult<>(403, "令牌已过期, 请重新登录"));
            return;
        }

        //Parse the token
        String username = JwtTokenUtil.getUsername(token);
        List<String> tokenRoles = JwtTokenUtil.getTokenRoles(token);
        ArrayList<SimpleGrantedAuthority> roles = new ArrayList<>();
        for (String role : tokenRoles) {
            roles.add(new SimpleGrantedAuthority(role));
        }
        //Store the authentication in the Spring Security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username,null, roles));

        super.doFilterInternal(request, response, chain);
    }

}
