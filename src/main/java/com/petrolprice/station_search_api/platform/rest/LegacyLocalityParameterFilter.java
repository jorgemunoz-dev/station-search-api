package com.petrolprice.station_search_api.platform.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class LegacyLocalityParameterFilter extends OncePerRequestFilter {
    private static final Set<String> HIERARCHY_ENDPOINTS = Set.of(
            "/stations", "/statistics/fuel-prices/current", "/statistics/fuel-prices/history");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (HIERARCHY_ENDPOINTS.contains(request.getRequestURI()) && request.getParameterMap().containsKey("locality")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Invalid geographic filter\",\"detail\":\"locality is not supported; use the ordered administrative hierarchy\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
