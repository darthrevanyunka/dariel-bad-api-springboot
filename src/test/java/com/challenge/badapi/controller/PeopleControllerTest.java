package com.challenge.badapi.controller;

import com.challenge.badapi.model.Person;
import com.challenge.badapi.service.BadBehaviorService;
import com.challenge.badapi.service.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PeopleController.class)
class PeopleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataService dataService;

    @MockBean
    private BadBehaviorService badBehaviorService;

    private List<Person> testPeople;

    @BeforeEach
    void setUp() {
        testPeople = Arrays.asList(
                new Person(1L, "John", "Smith", 25),
                new Person(2L, "Mary", "Johnson", 30)
        );
    }

    @Test
    void testGetFirstNames() throws Exception {
        when(dataService.getAllPeople()).thenReturn(testPeople);
        doNothing().when(badBehaviorService).maybeThrowRandomError();

        mockMvc.perform(get("/api/people/firstnames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].firstName").value("John"));

        verify(badBehaviorService).maybeThrowRandomError();
    }

    @Test
    void testGetSurnames() throws Exception {
        when(dataService.getAllPeople()).thenReturn(testPeople);
        doNothing().when(badBehaviorService).enforceRateLimit(anyString());

        mockMvc.perform(get("/api/people/surnames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].surname").value("Smith"));

        verify(badBehaviorService).enforceRateLimit(anyString());
    }

    @Test
    void testGetAges() throws Exception {
        when(badBehaviorService.parseCursor(any())).thenReturn(0);
        when(badBehaviorService.getPageSize()).thenReturn(50);
        when(dataService.getTotalCount()).thenReturn(1000);
        when(dataService.getPeoplePage(0, 50)).thenReturn(testPeople);
        when(badBehaviorService.generateNextCursor(0, 50, 1000)).thenReturn("50");
        when(badBehaviorService.hasMorePages(0, 50, 1000)).thenReturn(true);

        mockMvc.perform(get("/api/people/ages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.cursor").value("50"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void testGetAgesWithCursor() throws Exception {
        when(badBehaviorService.parseCursor("50")).thenReturn(50);
        when(badBehaviorService.getPageSize()).thenReturn(50);
        when(dataService.getTotalCount()).thenReturn(1000);
        when(dataService.getPeoplePage(50, 50)).thenReturn(testPeople);
        when(badBehaviorService.generateNextCursor(50, 50, 1000)).thenReturn("100");
        when(badBehaviorService.hasMorePages(50, 50, 1000)).thenReturn(true);

        mockMvc.perform(get("/api/people/ages?cursor=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursor").value("100"));
    }
}

