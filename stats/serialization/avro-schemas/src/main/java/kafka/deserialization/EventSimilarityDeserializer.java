package kafka.deserialization;

import ru.practicum.stats.avro.EventSimilarityAvro;

public class EventSimilarityDeserializer extends AvroDeserializer<EventSimilarityAvro> {

    public EventSimilarityDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}