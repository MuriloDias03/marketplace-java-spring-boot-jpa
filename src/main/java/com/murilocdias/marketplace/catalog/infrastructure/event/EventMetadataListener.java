package com.murilocdias.marketplace.catalog.infrastructure.event;

import com.murilocdias.marketplace.catalog.infrastructure.persistence.entity.EventMetadata;
import com.murilocdias.marketplace.common.infrastructure.event.dto.EventUpdated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class EventMetadataListener extends AbstractMongoEventListener<EventMetadata> {

    private static final Logger logger = LoggerFactory.getLogger(EventMetadataListener.class);

    private final ApplicationEventPublisher publisher;

    public EventMetadataListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onAfterSave(AfterSaveEvent<EventMetadata> event) {
        logger.info("Event metadata save via onAfterSave {}", event.getDocument());
        this.publisher.publishEvent(EventUpdated.from(event.getSource()));
    }

    @Override
    public void onAfterDelete(AfterDeleteEvent<EventMetadata> event) {
        logger.info("Event metadata delete via onAfterDelete {}", event.getDocument());
    }

}