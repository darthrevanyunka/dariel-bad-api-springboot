package com.challenge.badapi.controller;

import com.challenge.badapi.model.Submission;
import com.challenge.badapi.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Leaderboard", description = "View challenge statistics and winners")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @Value("${badapi.admin-key}")
    private String adminKey;

    @GetMapping("/leaderboard")
    @Operation(
            summary = "Get the leaderboard",
            description = "Returns all successful submissions sorted by submission time. First submission wins!"
    )
    public ResponseEntity<List<Submission>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getLeaderboard());
    }

    @GetMapping("/stats")
    @Operation(
            summary = "Get challenge statistics",
            description = "Returns statistics about total submissions, successful submissions, and participants"
    )
    public ResponseEntity<Map<String, Object>> getStats() {
        LeaderboardService.LeaderboardStats stats = leaderboardService.getStats();
        Map<String, Object> response = new HashMap<>();
        response.put("totalSubmissions", stats.totalSubmissions);
        response.put("correctSubmissions", stats.correctSubmissions);
        response.put("uniqueParticipants", stats.uniqueParticipants);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Reset the challenge",
            description = "Admin endpoint to reset all submissions and start fresh"
    )
    public ResponseEntity<Map<String, String>> resetChallenge(
            @RequestParam(required = false, defaultValue = "") String adminKey
    ) {
        // Simple admin key check (in production, use proper authentication)
        if (!this.adminKey.equals(adminKey)) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Unauthorized - invalid admin key"));
        }

        leaderboardService.resetChallenge();
        return ResponseEntity.ok(Map.of("message", "Challenge reset successfully"));
    }
}

