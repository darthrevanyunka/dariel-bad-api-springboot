package com.challenge.badapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private String participantName;
    private LocalDateTime submittedAt;
    private boolean isCorrect;
    private int totalRecords;
    private int correctRecords;
    private String message;
}

