package com.murilocdias.marketplace.ticketing.infrastructure.http.Request;

import com.murilocdias.marketplace.ticketing.domain.SeatId;

public record SeatSelectionRequest(
        String id
) {

    public SeatId toInput() {
        return new SeatId(id);
    }

}