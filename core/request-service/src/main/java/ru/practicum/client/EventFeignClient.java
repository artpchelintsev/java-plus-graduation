package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.event.EventFullDto;

@FeignClient(
        name = "event-service",
        fallback = EventFeignClientFallback.class
)
public interface EventFeignClient {

    @GetMapping("/events/{eventId}")
    EventFullDto getEventById(@PathVariable Long eventId);
}
