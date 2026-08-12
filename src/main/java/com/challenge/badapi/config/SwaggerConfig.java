package com.challenge.badapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI badApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bad API Challenge")
                        .version("1.0.0")
                        .description("""
                                # Welcome to the Bad API Challenge!
                                
                                This API is intentionally unreliable to teach resilient programming practices.
                                
                                ## Your Mission
                                Collect data for 2,000 people by calling the endpoints below, compute the required value,
                                and submit a CSV file. First correct submission wins!
                                
                                ## Expected CSV Format
                                ```
                                firstName,surname,age,computedValue
                                John,Smith,25,John25Smith
                                ...
                                (2,000 total records)
                                ```
                                
                                ## Computed Value Formula
                                `computedValue = firstName + age + surname`
                                
                                ## Known Issues
                                This API may experience various problems common in production environments:
                                - **Random HTTP Errors**: 5xx errors, service unavailable responses
                                - **Rate Limiting**: Too many requests may be throttled (429 responses)
                                - **Pagination**: Some endpoints may require cursor-based pagination
                                - **Network Issues**: Timeouts, slow responses, intermittent failures
                                
                                **Hint**: Implement retry logic, exponential backoff, and proper error handling!
                                
                                Good luck! 🚀
                                """)
                        .contact(new Contact()
                                .name("Challenge Admin")
                                .email("admin@challenge.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local server")
                ));
    }
}

