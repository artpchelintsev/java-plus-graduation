package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserBatchDto;
import ru.practicum.dto.user.UserDto;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public UserBatchDto getUsersByIds(List<Long> userIds) {
        log.warn("User Service unavailable, returning empty batch");
        return UserBatchDto.builder()
                .users(new HashMap<>())
                .build();
    }
    @Override
    public UserDto getUserById(Long userId) {
        log.error("User Service unavailable for user: {}", userId);
        throw new RuntimeException("User Service is temporarily unavailable");
    }
}
