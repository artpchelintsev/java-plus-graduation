package ru.practicum.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign error: method={}, status={}, reason={}",
                methodKey, response.status(), response.reason());

        if (response.status() == 404) {
            return new NotFoundException("Resource not found");
        } else if (response.status() == 409) {
            return new ConflictException("Conflict in downstream service");
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
