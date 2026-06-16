package com.murilocdias.marketplace.ticketing.infrastructure.http;

import com.murilocdias.marketplace.ticketing.application.SelectSeatUseCase;
import com.murilocdias.marketplace.ticketing.domain.CustomerId;
import com.murilocdias.marketplace.ticketing.domain.EventId;
import com.murilocdias.marketplace.ticketing.infrastructure.http.Request.SeatSelectionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/ticketing/events/{eventId}/seats")
public class SeatSelectionController {

    private final SelectSeatUseCase selectSeatUseCase;

    public SeatSelectionController(SelectSeatUseCase selectSeatUseCase) {
        this.selectSeatUseCase = selectSeatUseCase;
    }

    @PostMapping("/select")
    @ResponseStatus(HttpStatus.CREATED)
    public void selectSeat(@PathVariable String eventId,
                           @RequestBody SeatSelectionRequest request,
                           @RequestHeader("X-CUSTOMER-ID") String customerId) {
        selectSeatUseCase.execute(new EventId(UUID.fromString(eventId)), request.toInput(), new CustomerId(UUID.fromString(customerId)));
    }

}