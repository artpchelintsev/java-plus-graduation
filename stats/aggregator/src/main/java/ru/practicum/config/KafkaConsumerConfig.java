package ru.practicum.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.mapper.YamlMapper;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final YamlMapper yamlMapper;

    @Bean
    public ConsumerFactory<String, UserActionAvro> consumerFactory() {
        Map<String, Object> props = yamlMapper.flattenMap(kafkaProperties.getConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> kafkaListenerContainerFactory(
            ConsumerFactory<String, UserActionAvro> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

}