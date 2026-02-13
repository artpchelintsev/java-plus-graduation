package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.exception.NotFoundException;

@Component
@Slf4j
public class EventFeignClientFallback implements EventFeignClient {

    @Override
    public EventFullDto getEventById(Long eventId) {
        log.error("Event Service unavailable for event: {}", eventId);
        throw new NotFoundException("Event Service is temporarily unavailable");
    }
}
