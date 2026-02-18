package ru.practicum.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 404:
                return new NotFoundException("Resource not found");
            case 409:
                return new ConflictException("Conflict occurred");
            default:
                return defaultDecoder.decode(methodKey, response);
        }
    }
}
