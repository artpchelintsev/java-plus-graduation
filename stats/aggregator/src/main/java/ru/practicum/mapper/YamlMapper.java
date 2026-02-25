package ru.practicum.mapper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class YamlMapper {

    public Map<String, Object> flattenMap(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> {
            if (value instanceof Map) {
                ((Map<?, ?>) value).forEach((subKey, subValue) ->
                        result.put(key + "." + subKey, subValue));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

}