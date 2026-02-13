package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventBatchDto;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class EventFeignClientFallback implements EventFeignClient {

    @Override
    public EventBatchDto getEventsByIds(List<Long> eventIds) {
        log.warn("Event Service unavailable, returning empty events");
        return EventBatchDto.builder()
                .events(new HashMap<>())
                .build();
    }
}
