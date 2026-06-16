package com.murilocdias.marketplace.ticketing.infrastructure.event;

import com.murilocdias.marketplace.common.infrastructure.event.dto.CustomerCreated;
import com.murilocdias.marketplace.common.infrastructure.event.dto.EventUpdated;
import com.murilocdias.marketplace.ticketing.application.CreateCustomerUseCase;
import com.murilocdias.marketplace.ticketing.application.CreateEventUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TicketingEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TicketingEventListener.class);

    private final CreateCustomerUseCase createCustomerUseCase;
    private final CreateEventUseCase createEventUseCase;

    public TicketingEventListener(CreateCustomerUseCase createCustomerUseCase, CreateEventUseCase createEventUseCase) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.createEventUseCase = createEventUseCase;
    }

    @EventListener
    @Async
    public void handle(CustomerCreated event) {
        logger.info("CustomerCreated received {}", event);
        createCustomerUseCase.execute(event);
    }

    @EventListener
    @Async
    public void handle(EventUpdated event) {
        logger.info("EventUpdated received {}", event);
        createEventUseCase.execute(event);
    }

}