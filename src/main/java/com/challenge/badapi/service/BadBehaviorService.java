package com.challenge.badapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BadBehaviorService {

    private static final Logger log = LoggerFactory.getLogger(BadBehaviorService.class);

    @Value("${badapi.random-error.failure-rate:0.3}")
    private double failureRate;

    @Value("${badapi.rate-limit.requests-per-minute:50}")
    private int requestsPerMinute;

    @Value("${badapi.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${badapi.pagination.page-size:50}")
    private int pageSize;

    // Network Chaos Configuration
    @Value("${badapi.chaos.random-slowness-enabled:true}")
    private boolean randomSlownessEnabled;

    @Value("${badapi.chaos.slowness-probability:0.4}")
    private double slownessProbability;

    @Value("${badapi.chaos.min-delay-seconds:5}")
    private int minDelaySeconds;

    @Value("${badapi.chaos.max-delay-seconds:30}")
    private int maxDelaySeconds;

    @Value("${badapi.chaos.timeout-probability:0.15}")
    private double timeoutProbability;

    @Value("${badapi.chaos.variable-rate-limit:true}")
    private boolean variableRateLimit;

    @Value("${badapi.chaos.min-rate-limit:5}")
    private int minRateLimit;

    @Value("${badapi.chaos.max-rate-limit:100}")
    private int maxRateLimit;

    private final Random random = new Random();
    
    // Dynamic rate limits per client
    private final Map<String, Integer> clientRateLimits = new ConcurrentHashMap<>();
    
    // Rate limiting storage: key is identifier (IP/session), value is list of timestamps
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * Simulates random errors like in an overloaded production system
     */
    public void maybeThrowRandomError() {
        if (random.nextDouble() < failureRate) {
            int errorType = random.nextInt(4);
            switch (errorType) {
                case 0:
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error: Database connection timeout"
                    );
                case 1:
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Service Temporarily Unavailable: High load detected"
                    );
                case 2:
                    throw new ResponseStatusException(
                            HttpStatus.GATEWAY_TIMEOUT,
                            "Gateway Timeout: Upstream service not responding"
                    );
                case 3:
                    // Circuit breaker scenario - needs multiple retries
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Circuit breaker open: Service degraded"
                    );
            }
        }
    }

    /**
     * Simulate random network slowness (Network Chaos #6)
     */
    public void maybeSlowDown() {
        if (!randomSlownessEnabled) {
            return;
        }

        if (random.nextDouble() < slownessProbability) {
            int delaySeconds = minDelaySeconds + random.nextInt(maxDelaySeconds - minDelaySeconds + 1);
            log.info("🐌 Simulating slow response: {}s delay", delaySeconds);
            
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Simulate timeout by sleeping indefinitely (Network Chaos #6)
     * Client must implement proper timeout handling
     */
    public void maybeTimeout() {
        if (random.nextDouble() < timeoutProbability) {
            log.info("⏱️  Simulating timeout: No response will be sent");
            
            try {
                // Sleep for a very long time to simulate timeout
                Thread.sleep(120000); // 2 minutes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            throw new ResponseStatusException(
                    HttpStatus.REQUEST_TIMEOUT,
                    "Request timeout"
            );
        }
    }

    /**
     * Enforces rate limiting per client identifier with variable limits (Behavior #2)
     */
    public void enforceRateLimit(String clientIdentifier) {
        long now = Instant.now().getEpochSecond();
        
        // Get or assign a random rate limit for this client
        int currentLimit = requestsPerMinute;
        if (variableRateLimit) {
            currentLimit = clientRateLimits.computeIfAbsent(clientIdentifier, k -> {
                int limit = minRateLimit + random.nextInt(maxRateLimit - minRateLimit + 1);
                log.info("🎲 Assigned rate limit of {} req/min to client: {}", limit, clientIdentifier);
                return limit;
            });
            
            // Occasionally change the rate limit to make it unpredictable
            if (random.nextDouble() < 0.1) { // 10% chance to change
                currentLimit = minRateLimit + random.nextInt(maxRateLimit - minRateLimit + 1);
                clientRateLimits.put(clientIdentifier, currentLimit);
                log.info("🔄 Changed rate limit to {} req/min for client: {}", currentLimit, clientIdentifier);
            }
        }
        
        final int effectiveLimit = currentLimit;
        
        rateLimitMap.compute(clientIdentifier, (key, info) -> {
            if (info == null) {
                info = new RateLimitInfo();
            }
            
            // Clean up old timestamps outside the window
            info.timestamps.removeIf(timestamp -> timestamp < now - windowSeconds);
            
            // Check if limit exceeded
            if (info.timestamps.size() >= effectiveLimit) {
                long oldestRequest = info.timestamps.get(0);
                long retryAfter = windowSeconds - (now - oldestRequest);
                
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        String.format("Rate limit exceeded (%d req/min). Retry after %d seconds", 
                                effectiveLimit, retryAfter)
                );
            }
            
            // Add current request
            info.timestamps.add(now);
            return info;
        });
    }

    /**
     * Validates and parses cursor for pagination
     */
    public int parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        
        try {
            int offset = Integer.parseInt(cursor);
            if (offset < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid cursor: must be non-negative"
                );
            }
            return offset;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid cursor format: must be a number"
            );
        }
    }

    /**
     * Generates next cursor for pagination
     */
    public String generateNextCursor(int currentOffset, int pageSize, int totalRecords) {
        int nextOffset = currentOffset + pageSize;
        if (nextOffset >= totalRecords) {
            return null;
        }
        return String.valueOf(nextOffset);
    }

    /**
     * Returns whether there are more pages
     */
    public boolean hasMorePages(int currentOffset, int pageSize, int totalRecords) {
        return currentOffset + pageSize < totalRecords;
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * Helper class to track rate limit info
     */
    private static class RateLimitInfo {
        private final java.util.List<Long> timestamps = new java.util.ArrayList<>();
    }

    /**
     * Clean up old rate limit data periodically (call this from a scheduled task if needed)
     */
    public void cleanupRateLimitData() {
        long now = Instant.now().getEpochSecond();
        rateLimitMap.entrySet().removeIf(entry -> {
            entry.getValue().timestamps.removeIf(timestamp -> timestamp < now - windowSeconds);
            return entry.getValue().timestamps.isEmpty();
        });
    }
}

