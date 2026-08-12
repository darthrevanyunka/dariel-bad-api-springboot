package com.challenge.badapi.service;

import com.challenge.badapi.model.Person;
import com.challenge.badapi.model.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationServiceTest {

    // Must match ValidationService.EXPECTED_RECORD_COUNT so a full, correct
    // submission is actually recognized as complete.
    private static final int EXPECTED_RECORD_COUNT = 2000;

    @Mock
    private DataService dataService;

    @InjectMocks
    private ValidationService validationService;

    private List<Person> testPeople;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test data - names are unique per index so no collisions
        testPeople = new ArrayList<>();
        for (long i = 1; i <= EXPECTED_RECORD_COUNT; i++) {
            testPeople.add(new Person(i, "First" + i, "Last" + i, 20 + (int)(i % 63)));
        }

        when(dataService.getAllPeople()).thenReturn(testPeople);
    }

    @Test
    void testValidSubmission() {
        // Create valid CSV
        StringBuilder csv = new StringBuilder();
        csv.append("firstName,surname,age,computedValue\n");
        for (Person person : testPeople) {
            csv.append(String.format("%s,%s,%d,%s\n",
                    person.getFirstName(),
                    person.getSurname(),
                    person.getAge(),
                    person.getComputedValue()));
        }

        Submission result = validationService.validateSubmission("TestUser", csv.toString());

        assertNotNull(result);
        assertEquals("TestUser", result.getParticipantName());
        assertTrue(result.isCorrect());
        assertEquals(EXPECTED_RECORD_COUNT, result.getTotalRecords());
        assertEquals(EXPECTED_RECORD_COUNT, result.getCorrectRecords());
    }

    @Test
    void testInvalidComputedValue() {
        StringBuilder csv = new StringBuilder();
        csv.append("firstName,surname,age,computedValue\n");
        // Add first person with wrong computed value
        Person person = testPeople.get(0);
        csv.append(String.format("%s,%s,%d,WrongValue\n",
                person.getFirstName(),
                person.getSurname(),
                person.getAge()));

        Submission result = validationService.validateSubmission("TestUser", csv.toString());

        assertFalse(result.isCorrect());
        assertEquals(1, result.getTotalRecords());
        assertEquals(0, result.getCorrectRecords());
    }

    @Test
    void testEmptySubmission() {
        Submission result = validationService.validateSubmission("TestUser", "");

        assertFalse(result.isCorrect());
        assertEquals(0, result.getTotalRecords());
        assertTrue(result.getMessage().contains("empty"));
    }

    @Test
    void testInvalidHeader() {
        String csv = "wrong,header,format\ndata1,data2,data3\n";

        Submission result = validationService.validateSubmission("TestUser", csv);

        assertFalse(result.isCorrect());
        assertTrue(result.getMessage().contains("CSV") || result.getMessage().contains("header"));
    }

    @Test
    void testIncorrectRecordCount() {
        // Create CSV with only 5 records when EXPECTED_RECORD_COUNT is expected
        StringBuilder csv = new StringBuilder();
        csv.append("firstName,surname,age,computedValue\n");
        for (int i = 0; i < 5; i++) {
            Person person = testPeople.get(i);
            csv.append(String.format("%s,%s,%d,%s\n",
                    person.getFirstName(),
                    person.getSurname(),
                    person.getAge(),
                    person.getComputedValue()));
        }

        Submission result = validationService.validateSubmission("TestUser", csv.toString());

        assertFalse(result.isCorrect());
        assertEquals(5, result.getTotalRecords());
    }
}

