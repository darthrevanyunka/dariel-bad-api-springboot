package com.challenge.badapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ApiResponse<T> {
    private List<T> data;
    private String cursor;
    private boolean hasMore;
    private int total;

    public ApiResponse(List<T> data) {
        this.data = data;
        this.cursor = null;
        this.hasMore = false;
        this.total = data.size();
    }

    public ApiResponse(List<T> data, String cursor, boolean hasMore, int total) {
        this.data = data;
        this.cursor = cursor;
        this.hasMore = hasMore;
        this.total = total;
    }
}

