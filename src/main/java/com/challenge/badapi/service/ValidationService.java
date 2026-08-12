package com.challenge.badapi.service;

import com.challenge.badapi.model.Person;
import com.challenge.badapi.model.Submission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ValidationService {

    private static final int EXPECTED_RECORD_COUNT = 2000;

    @Autowired
    private DataService dataService;

    /**
     * Validates a CSV submission
     * Expected format: firstName,surname,age,computedValue
     * Computed value formula: firstName + age + surname
     */
    public Submission validateSubmission(String participantName, String csvContent) {
        Submission submission = new Submission();
        submission.setParticipantName(participantName);
        submission.setSubmittedAt(LocalDateTime.now());

        try {
            // Parse CSV
            List<CsvRow> rows = parseCsv(csvContent);
            
            if (rows.isEmpty()) {
                submission.setCorrect(false);
                submission.setTotalRecords(0);
                submission.setCorrectRecords(0);
                submission.setMessage("CSV is empty or has no valid data rows");
                return submission;
            }

            // Check if we have exactly the expected number of records
            submission.setTotalRecords(rows.size());

            if (rows.size() != EXPECTED_RECORD_COUNT) {
                submission.setCorrect(false);
                submission.setCorrectRecords(0);
                submission.setMessage(String.format("Expected %d records, but found %d", EXPECTED_RECORD_COUNT, rows.size()));
                return submission;
            }

            // Validate each row against the expected data
            int correctCount = 0;
            List<String> errors = new ArrayList<>();
            Map<Long, Person> expectedPeople = new HashMap<>();
            
            for (Person person : dataService.getAllPeople()) {
                expectedPeople.put(person.getId(), person);
            }

            Set<Long> seenIds = new HashSet<>();
            
            for (int i = 0; i < rows.size(); i++) {
                CsvRow row = rows.get(i);
                
                // Find matching person by all attributes
                Person matchedPerson = findMatchingPerson(row, expectedPeople);
                
                if (matchedPerson == null) {
                    if (errors.size() < 5) { // Only collect first 5 errors
                        errors.add(String.format("Row %d: No matching person found for data: %s,%s,%d,%s",
                                i + 1, row.firstName, row.surname, row.age, row.computedValue));
                    }
                    continue;
                }
                
                // Check for duplicates
                if (seenIds.contains(matchedPerson.getId())) {
                    if (errors.size() < 5) {
                        errors.add(String.format("Row %d: Duplicate entry for person ID %d",
                                i + 1, matchedPerson.getId()));
                    }
                    continue;
                }
                
                // Validate computed value
                String expectedComputed = matchedPerson.getComputedValue();
                if (!expectedComputed.equals(row.computedValue)) {
                    if (errors.size() < 5) {
                        errors.add(String.format("Row %d: Incorrect computed value. Expected '%s' but got '%s'",
                                i + 1, expectedComputed, row.computedValue));
                    }
                    continue;
                }
                
                seenIds.add(matchedPerson.getId());
                correctCount++;
            }

            submission.setCorrectRecords(correctCount);
            
            if (correctCount == EXPECTED_RECORD_COUNT && seenIds.size() == EXPECTED_RECORD_COUNT) {
                submission.setCorrect(true);
                submission.setMessage(String.format("Perfect! All %d records are correct!", EXPECTED_RECORD_COUNT));
            } else {
                submission.setCorrect(false);
                String errorMsg = String.format(
                        "Validation failed. %d out of %d records are correct. %d unique people found.",
                        correctCount, EXPECTED_RECORD_COUNT, seenIds.size()
                );
                if (!errors.isEmpty()) {
                    errorMsg += " Sample errors: " + String.join("; ", errors);
                }
                submission.setMessage(errorMsg);
            }

        } catch (Exception e) {
            submission.setCorrect(false);
            submission.setTotalRecords(0);
            submission.setCorrectRecords(0);
            submission.setMessage("Error parsing CSV: " + e.getMessage());
        }

        return submission;
    }

    /**
     * Finds a person that matches the CSV row data
     */
    private Person findMatchingPerson(CsvRow row, Map<Long, Person> expectedPeople) {
        for (Person person : expectedPeople.values()) {
            if (person.getFirstName().equals(row.firstName) &&
                person.getSurname().equals(row.surname) &&
                person.getAge().equals(row.age)) {
                return person;
            }
        }
        return null;
    }

    /**
     * Parses CSV content into rows
     */
    private List<CsvRow> parseCsv(String csvContent) throws Exception {
        List<CsvRow> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                // Skip header
                if (isHeader) {
                    isHeader = false;
                    // Validate header format
                    if (!line.toLowerCase().contains("firstname") || 
                        !line.toLowerCase().contains("surname") || 
                        !line.toLowerCase().contains("age") || 
                        !line.toLowerCase().contains("computedvalue")) {
                        throw new IllegalArgumentException(
                            "Invalid CSV header. Expected: firstName,surname,age,computedValue"
                        );
                    }
                    continue;
                }
                
                // Parse data row
                String[] parts = line.split(",", -1);
                if (parts.length != 4) {
                    throw new IllegalArgumentException(
                        "Invalid CSV row format. Expected 4 columns, found: " + parts.length
                    );
                }
                
                try {
                    CsvRow row = new CsvRow();
                    row.firstName = parts[0].trim();
                    row.surname = parts[1].trim();
                    row.age = Integer.parseInt(parts[2].trim());
                    row.computedValue = parts[3].trim();
                    rows.add(row);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid age value: " + parts[2]);
                }
            }
        }
        
        return rows;
    }

    /**
     * Helper class for CSV row data
     */
    private static class CsvRow {
        String firstName;
        String surname;
        Integer age;
        String computedValue;
    }
}

