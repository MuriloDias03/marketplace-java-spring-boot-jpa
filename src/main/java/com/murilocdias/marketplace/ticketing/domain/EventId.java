package com.murilocdias.marketplace.ticketing.domain;

import java.util.UUID;

public record EventId(
        UUID id
) {

    public EventId() {
        this(UUID.randomUUID());
    }

}