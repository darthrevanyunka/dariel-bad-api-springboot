package com.challenge.badapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private Long id;
    private String firstName;
    private String surname;
    private Integer age;

    public String getComputedValue() {
        return firstName + age + surname;
    }
}

