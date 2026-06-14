package com.murilocdias.marketplace.ticketing.infrastructure.event;

import com.murilocdias.marketplace.common.infrastructure.event.dto.CustomerCreated;
import com.murilocdias.marketplace.common.infrastructure.event.dto.EventUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TicketingEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TicketingEventListener.class);

    @EventListener
    @Async
    public void handle(CustomerCreated event) {
        logger.info("CustomerCreated received {}", event);
    }

    @EventListener
    @Async
    public void handle(EventUpdated event) {
        logger.info("CustomerUpdated received {}", event);
    }

}