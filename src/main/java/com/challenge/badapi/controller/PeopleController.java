package com.challenge.badapi.controller;

import com.challenge.badapi.model.ApiResponse;
import com.challenge.badapi.model.Person;
import com.challenge.badapi.service.BadBehaviorService;
import com.challenge.badapi.service.DataService;
import com.challenge.badapi.util.ClientIpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/people")
@Tag(name = "People Data", description = "Endpoints to retrieve person data (first names, surnames, ages)")
public class PeopleController {

    @Autowired
    private DataService dataService;

    @Autowired
    private BadBehaviorService badBehaviorService;

    @GetMapping("/firstnames")
    @Operation(
            summary = "Get first names",
            description = "Returns a list of person IDs and their first names. " +
                    "This endpoint may experience intermittent errors, random slowness, or timeouts."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved first names",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Service unavailable"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "408",
                    description = "Request timeout"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFirstNames(HttpServletRequest request) {
        // Network chaos: maybe timeout
        badBehaviorService.maybeTimeout();
        
        // Network chaos: random slowness
        badBehaviorService.maybeSlowDown();
        
        // This endpoint has random errors
        badBehaviorService.maybeThrowRandomError();

        List<Person> people = dataService.getAllPeople();
        
        List<Map<String, Object>> data = people.stream()
                .map(person -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", person.getId());
                    map.put("firstName", person.getFirstName());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/surnames")
    @Operation(
            summary = "Get surnames",
            description = "Returns a list of person IDs and their surnames. " +
                    "This endpoint has unpredictable rate limiting that varies per client and may change randomly. " +
                    "May also experience slowness or timeouts."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved surnames",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Too many requests - rate limit exceeded (limit varies!)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "408",
                    description = "Request timeout"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSurnames(HttpServletRequest request) {
        String clientId = ClientIpUtils.getClientIp(request);
        
        // Network chaos: maybe timeout
        badBehaviorService.maybeTimeout();
        
        // Network chaos: random slowness
        badBehaviorService.maybeSlowDown();
        
        // This endpoint has variable rate limiting
        badBehaviorService.enforceRateLimit(clientId);

        List<Person> people = dataService.getAllPeople();
        
        List<Map<String, Object>> data = people.stream()
                .map(person -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", person.getId());
                    map.put("surname", person.getSurname());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(data));
    }

    @GetMapping("/ages")
    @Operation(
            summary = "Get ages (paginated)",
            description = "Returns a paginated list of person IDs and their ages. " +
                    "Requires cursor-based pagination. Use the cursor from the response to get the next page. " +
                    "May experience slowness or timeouts."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved ages page",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid cursor parameter"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "408",
                    description = "Request timeout"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAges(
            @Parameter(description = "Pagination cursor (offset)", example = "0")
            @RequestParam(required = false) String cursor,
            HttpServletRequest request
    ) {
        // Network chaos: maybe timeout
        badBehaviorService.maybeTimeout();
        
        // Network chaos: random slowness
        badBehaviorService.maybeSlowDown();
        
        // This endpoint requires cursor-based pagination
        int offset = badBehaviorService.parseCursor(cursor);
        int pageSize = badBehaviorService.getPageSize();
        int totalRecords = dataService.getTotalCount();

        List<Person> peoplePage = dataService.getPeoplePage(offset, pageSize);
        
        List<Map<String, Object>> data = peoplePage.stream()
                .map(person -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", person.getId());
                    map.put("age", person.getAge());
                    return map;
                })
                .collect(Collectors.toList());

        String nextCursor = badBehaviorService.generateNextCursor(offset, pageSize, totalRecords);
        boolean hasMore = badBehaviorService.hasMorePages(offset, pageSize, totalRecords);

        ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                data,
                nextCursor,
                hasMore,
                totalRecords
        );

        return ResponseEntity.ok(response);
    }
}

