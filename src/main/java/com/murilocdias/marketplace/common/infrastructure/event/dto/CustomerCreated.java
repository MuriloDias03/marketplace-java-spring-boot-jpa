package com.murilocdias.marketplace.common.infrastructure.event.dto;

public record CustomerCreated(
        String id,
        String name
) {}