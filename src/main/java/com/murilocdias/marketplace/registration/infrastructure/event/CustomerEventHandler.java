package com.murilocdias.marketplace.registration.infrastructure.event;

import com.murilocdias.marketplace.common.infrastructure.event.dto.CustomerCreated;
import com.murilocdias.marketplace.registration.infrastructure.persistence.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class CustomerEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEventHandler.class);

    private final ApplicationEventPublisher publisher;

    public CustomerEventHandler(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @HandleAfterCreate
    public void handleAfterCreate(Customer customer) {
        logger.warn("CustomerEventHandler#handleAfterCreate");
        publisher.publishEvent(new CustomerCreated(customer.getId().toString(), customer.getFirstName()));
    }

    @HandleAfterSave
    public void HandleAfterSave(Customer customer) {
        logger.warn("CustomerEventHandler#handleAfterSave");
    }

    @HandleAfterDelete
    public void HandleAfterDelete(Customer customer) {
        logger.warn("CustomerEventHandler#handleAfterDelete");
    }

}