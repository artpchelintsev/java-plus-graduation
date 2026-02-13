package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserBatchDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        fallback = UserFeignClientFallback.class
)
public interface UserFeignClient {

    @PostMapping("/admin/users/batch")
    UserBatchDto getUsersByIds(@RequestBody List<Long> userIds);
}
