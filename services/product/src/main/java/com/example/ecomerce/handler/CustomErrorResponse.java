package com.example.ecomerce.handler;

import java.util.Map;

public record CustomErrorResponse(
        Map<String, String> errors
) {
}
