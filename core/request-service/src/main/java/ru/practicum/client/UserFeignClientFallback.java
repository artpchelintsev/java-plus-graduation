package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;
import ru.practicum.exception.NotFoundException;

@Component
@Slf4j
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public UserDto getUserById(Long userId) {
        log.error("User Service unavailable for user: {}", userId);
        throw new NotFoundException("User Service is temporarily unavailable");
    }
}
