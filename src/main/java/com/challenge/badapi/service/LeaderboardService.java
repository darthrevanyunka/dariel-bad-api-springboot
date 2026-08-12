package com.challenge.badapi.service;

import com.challenge.badapi.model.Submission;
import com.challenge.badapi.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardService {

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Get the leaderboard (all winners sorted by submission time)
     */
    public List<Submission> getLeaderboard() {
        return submissionRepository.getWinners();
    }

    /**
     * Check if a participant has already won
     */
    public boolean hasParticipantWon(String participantName) {
        return submissionRepository.hasWon(participantName);
    }

    /**
     * Get statistics
     */
    public LeaderboardStats getStats() {
        List<Submission> allSubmissions = submissionRepository.getAllSubmissions();
        long totalSubmissions = allSubmissions.size();
        long correctSubmissions = allSubmissions.stream().filter(Submission::isCorrect).count();
        long uniqueParticipants = allSubmissions.stream()
                .map(Submission::getParticipantName)
                .distinct()
                .count();

        return new LeaderboardStats(totalSubmissions, correctSubmissions, uniqueParticipants);
    }

    /**
     * Save a submission
     */
    public void saveSubmission(Submission submission) {
        submissionRepository.save(submission);
    }

    /**
     * Reset the challenge
     */
    public void resetChallenge() {
        submissionRepository.reset();
    }

    public static class LeaderboardStats {
        public long totalSubmissions;
        public long correctSubmissions;
        public long uniqueParticipants;

        public LeaderboardStats(long totalSubmissions, long correctSubmissions, long uniqueParticipants) {
            this.totalSubmissions = totalSubmissions;
            this.correctSubmissions = correctSubmissions;
            this.uniqueParticipants = uniqueParticipants;
        }
    }
}

