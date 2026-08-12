package com.challenge.badapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class BadBehaviorServiceTest {

    private BadBehaviorService badBehaviorService;

    @BeforeEach
    void setUp() {
        badBehaviorService = new BadBehaviorService();
        ReflectionTestUtils.setField(badBehaviorService, "failureRate", 1.0); // 100% failure for testing
        ReflectionTestUtils.setField(badBehaviorService, "requestsPerMinute", 2);
        ReflectionTestUtils.setField(badBehaviorService, "windowSeconds", 60);
        ReflectionTestUtils.setField(badBehaviorService, "pageSize", 50);
    }

    @Test
    void testMaybeThrowRandomError() {
        // With 100% failure rate, should always throw
        assertThrows(ResponseStatusException.class, () -> {
            badBehaviorService.maybeThrowRandomError();
        });
    }

    @Test
    void testRateLimitEnforcement() {
        String clientId = "test-client";

        // First request should succeed
        assertDoesNotThrow(() -> badBehaviorService.enforceRateLimit(clientId));

        // Second request should succeed
        assertDoesNotThrow(() -> badBehaviorService.enforceRateLimit(clientId));

        // Third request should fail (limit is 2)
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            badBehaviorService.enforceRateLimit(clientId);
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
    }

    @Test
    void testParseCursorValid() {
        int offset = badBehaviorService.parseCursor("100");
        assertEquals(100, offset);
    }

    @Test
    void testParseCursorNull() {
        int offset = badBehaviorService.parseCursor(null);
        assertEquals(0, offset);
    }

    @Test
    void testParseCursorEmpty() {
        int offset = badBehaviorService.parseCursor("");
        assertEquals(0, offset);
    }

    @Test
    void testParseCursorInvalid() {
        assertThrows(ResponseStatusException.class, () -> {
            badBehaviorService.parseCursor("invalid");
        });
    }

    @Test
    void testParseCursorNegative() {
        assertThrows(ResponseStatusException.class, () -> {
            badBehaviorService.parseCursor("-1");
        });
    }

    @Test
    void testGenerateNextCursor() {
        String cursor = badBehaviorService.generateNextCursor(0, 50, 100);
        assertEquals("50", cursor);
    }

    @Test
    void testGenerateNextCursorAtEnd() {
        String cursor = badBehaviorService.generateNextCursor(50, 50, 100);
        assertNull(cursor);
    }

    @Test
    void testHasMorePages() {
        assertTrue(badBehaviorService.hasMorePages(0, 50, 100));
        assertFalse(badBehaviorService.hasMorePages(50, 50, 100));
    }
}

