package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.request.RequestStatsDto;

import java.util.List;

@FeignClient(
        name = "request-service",
        fallback = RequestFeignClientFallback.class
)
public interface RequestFeignClient {

    @PostMapping("/requests/stats")
    RequestStatsDto getRequestStats(@RequestBody List<Long> eventIds);
}
