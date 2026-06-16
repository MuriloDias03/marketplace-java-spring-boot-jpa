package com.murilocdias.marketplace.ticketing.application;

import com.murilocdias.marketplace.common.infrastructure.event.dto.EventUpdated;
import com.murilocdias.marketplace.ticketing.domain.Event;
import com.murilocdias.marketplace.ticketing.domain.EventRepository;
import com.murilocdias.marketplace.ticketing.domain.Seat;
import com.murilocdias.marketplace.ticketing.domain.Sector;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CreateEventUseCase {

    private final EventRepository eventRepository;

    public CreateEventUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void execute(EventUpdated event) {
        Map<Sector, List<Seat>> seats = event.sectors().stream()
                .collect(Collectors.toMap(
                        sector -> new Sector(sector.id(), sector.price()),
                        sector -> sector.seats().stream()
                                .map(seatDto -> new Seat(seatDto.number()))
                                .toList()
                ));

        eventRepository.save(new Event(event.id(), seats));
    }
}