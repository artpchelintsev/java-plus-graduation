package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.request.RequestStatsDto;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class RequestFeignClientFallback implements RequestFeignClient {

    @Override
    public RequestStatsDto getRequestStats(List<Long> eventIds) {
        log.warn("Request Service unavailable, returning zeros");
        return RequestStatsDto.builder()
                .confirmedRequests(new HashMap<>())
                .build();
    }
}
