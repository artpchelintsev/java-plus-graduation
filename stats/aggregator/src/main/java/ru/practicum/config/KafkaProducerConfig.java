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
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;
    private final YamlMapper yamlMapper;

    @Bean
    public ProducerFactory<String, SpecificRecordBase> producerFactory() {
        Map<String, Object> props = yamlMapper.flattenMap(kafkaProperties.getProducer().getProperties());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, SpecificRecordBase> kafkaTemplate(ProducerFactory<String, SpecificRecordBase> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

}