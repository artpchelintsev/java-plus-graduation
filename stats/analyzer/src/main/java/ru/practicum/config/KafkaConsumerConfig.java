package ru.practicum.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.stats.avro.EventSimilarityAvro;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.mapper.YamlMapper;

import java.util.Map;


@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;
    private final YamlMapper yamlMapper;

    @Bean
    public ConsumerFactory<String, EventSimilarityAvro> similarityConsumerFactory() {
        Map<String, Object> props = yamlMapper.flattenMap(kafkaProperties.getSimilarityConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> similarityListenerContainerFactory(
            ConsumerFactory<String, EventSimilarityAvro> similarityConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(similarityConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UserActionAvro> actionConsumerFactory() {
        Map<String, Object> props = yamlMapper.flattenMap(kafkaProperties.getActionConsumer().getProperties());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> actionListenerContainerFactory(
            ConsumerFactory<String, UserActionAvro> actionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(actionConsumerFactory);
        return factory;
    }

}