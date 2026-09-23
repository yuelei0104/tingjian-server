package com.tingjian.server.dto;

public record HomePlanResponse(
        String code,
        String name,
        String description,
        boolean purchasable) {
}
