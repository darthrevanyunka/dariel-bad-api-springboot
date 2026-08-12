package com.challenge.badapi.repository;

import com.challenge.badapi.model.Submission;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Repository
public class SubmissionRepository {

    private final CopyOnWriteArrayList<Submission> submissions = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, Submission> winnersByParticipant = new ConcurrentHashMap<>();

    /**
     * Save a submission
     */
    public void save(Submission submission) {
        submissions.add(submission);
        
        // If this is a correct submission and participant doesn't have a winning submission yet
        if (submission.isCorrect()) {
            winnersByParticipant.putIfAbsent(submission.getParticipantName(), submission);
        }
    }

    /**
     * Get all correct submissions sorted by time (winners)
     */
    public List<Submission> getWinners() {
        return submissions.stream()
                .filter(Submission::isCorrect)
                .sorted(Comparator.comparing(Submission::getSubmittedAt))
                .collect(Collectors.toList());
    }

    /**
     * Get all submissions for a participant
     */
    public List<Submission> getSubmissionsByParticipant(String participantName) {
        return submissions.stream()
                .filter(s -> s.getParticipantName().equals(participantName))
                .sorted(Comparator.comparing(Submission::getSubmittedAt))
                .collect(Collectors.toList());
    }

    /**
     * Get all submissions
     */
    public List<Submission> getAllSubmissions() {
        return new ArrayList<>(submissions);
    }

    /**
     * Check if participant already won
     */
    public boolean hasWon(String participantName) {
        return winnersByParticipant.containsKey(participantName);
    }

    /**
     * Get total number of submissions
     */
    public int getTotalSubmissions() {
        return submissions.size();
    }

    /**
     * Reset all submissions (admin function)
     */
    public void reset() {
        submissions.clear();
        winnersByParticipant.clear();
    }
}

