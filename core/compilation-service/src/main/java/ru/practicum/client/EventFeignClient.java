package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventBatchDto;

import java.util.List;

@FeignClient(
        name = "event-service",
        fallback = EventFeignClientFallback.class
)
public interface EventFeignClient {

    @PostMapping("/events/batch")
    EventBatchDto getEventsByIds(@RequestBody List<Long> eventIds);
}
