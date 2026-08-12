package com.challenge.badapi.controller;

import com.challenge.badapi.model.Submission;
import com.challenge.badapi.service.LeaderboardService;
import com.challenge.badapi.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Challenge", description = "Submit your solution and view the leaderboard")
public class SubmissionController {

    @Autowired
    private ValidationService validationService;

    @Autowired
    private LeaderboardService leaderboardService;

    @PostMapping("/submit")
    @Operation(
            summary = "Submit your CSV solution",
            description = "Submit your completed CSV file with all 2,000 people records. " +
                    "The submission will be validated and you'll receive feedback on correctness."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Submission received and validated",
                    content = @Content(schema = @Schema(implementation = SubmissionResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid submission"
            )
    })
    public ResponseEntity<SubmissionResponse> submitSolution(
            @Parameter(description = "Your name/identifier", required = true)
            @RequestParam String participantName,
            @Parameter(description = "CSV file or content", required = true)
            @RequestParam String csvContent
    ) {
        if (participantName == null || participantName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new SubmissionResponse(false, "Participant name is required", null));
        }

        // Validate the submission
        Submission submission = validationService.validateSubmission(participantName.trim(), csvContent);
        
        // Save the submission
        leaderboardService.saveSubmission(submission);

        // Check if success rate is less than 10%
        double successRate = submission.getTotalRecords() > 0 
                ? (double) submission.getCorrectRecords() / submission.getTotalRecords() 
                : 0.0;

        SubmissionResponse response = new SubmissionResponse();
        response.setSuccess(submission.isCorrect());
        response.setMessage(submission.getMessage());
        response.setSubmission(submission);
        response.setSuccessRate(successRate);
        
        // If less than 10% correct, flag for GTFO page
        if (successRate < 0.10 && submission.getTotalRecords() > 0) {
            response.setRedirectToMeme(true);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/submit/file")
    @Operation(
            summary = "Submit your CSV file",
            description = "Submit your completed CSV file with all 2,000 people records via file upload."
    )
    public ResponseEntity<SubmissionResponse> submitFile(
            @Parameter(description = "Your name/identifier", required = true)
            @RequestParam String participantName,
            @Parameter(description = "CSV file", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        try {
            String csvContent = new String(file.getBytes());
            return submitSolution(participantName, csvContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new SubmissionResponse(false, "Error reading file: " + e.getMessage(), null));
        }
    }

    /**
     * Response model for submission
     */
    public static class SubmissionResponse {
        private boolean success;
        private String message;
        private Submission submission;
        private double successRate;
        private boolean redirectToMeme;

        public SubmissionResponse() {}

        public SubmissionResponse(boolean success, String message, Submission submission) {
            this.success = success;
            this.message = message;
            this.submission = submission;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Submission getSubmission() { return submission; }
        public void setSubmission(Submission submission) { this.submission = submission; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public boolean isRedirectToMeme() { return redirectToMeme; }
        public void setRedirectToMeme(boolean redirectToMeme) { this.redirectToMeme = redirectToMeme; }
    }
}

