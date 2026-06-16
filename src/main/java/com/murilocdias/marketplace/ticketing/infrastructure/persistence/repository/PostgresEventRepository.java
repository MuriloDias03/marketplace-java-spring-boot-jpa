package com.murilocdias.marketplace.ticketing.infrastructure.persistence.repository;

import com.murilocdias.marketplace.ticketing.domain.Event;
import com.murilocdias.marketplace.ticketing.domain.EventRepository;
import com.murilocdias.marketplace.ticketing.domain.Seat;
import com.murilocdias.marketplace.ticketing.domain.Sector;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostgresEventRepository implements EventRepository {

    private final EventCrudRepository eventCrudRepository;

    public PostgresEventRepository(EventCrudRepository eventCrudRepository) {
        this.eventCrudRepository = eventCrudRepository;
    }

    @Override
    public void save(Event event) {

        var sectors = event.getSeats().entrySet().stream()
                .map(entry -> {
                    Sector domainSector = entry.getKey();
                    List<Seat> domainSeats = entry.getValue();

                    var seats = domainSeats.stream()
                            .map(s -> new com.murilocdias.marketplace.ticketing.infrastructure.persistence.entity.Seat(
                                    s.getId(),
                                    s.getCorrelationId().id()
                            ))
                            .toList();

                    return new com.murilocdias.marketplace.ticketing.infrastructure.persistence.entity.Sector(
                            domainSector.getId(),
                            domainSector.getCorrelationId().id(),
                            domainSector.getPrice(),
                            seats
                    );
                })
                .toList();

        var entity = new com.murilocdias.marketplace.ticketing.infrastructure.persistence.entity.Event(
                event.getId(),
                event.getCorrelationId().id(),
                sectors
        );

        eventCrudRepository.save(entity);
    }

}
