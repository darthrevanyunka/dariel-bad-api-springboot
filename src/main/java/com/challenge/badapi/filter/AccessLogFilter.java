package com.challenge.badapi.filter;

import com.challenge.badapi.util.ClientIpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * Logs one line per request (client IP, method, path, status, duration) so
 * request activity can be traced by IP without instrumenting every controller.
 */
@Component
public class AccessLogFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            String clientIp = ClientIpUtils.getClientIp(httpRequest);
            String query = httpRequest.getQueryString();
            String path = query == null
                    ? httpRequest.getRequestURI()
                    : httpRequest.getRequestURI() + "?" + query;

            log.info("ACCESS {} {} {} -> {} ({}ms)",
                    clientIp,
                    httpRequest.getMethod(),
                    path,
                    httpResponse.getStatus(),
                    durationMs);
        }
    }
}
