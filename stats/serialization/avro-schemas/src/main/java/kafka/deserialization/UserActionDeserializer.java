package kafka.deserialization;

import ru.practicum.stats.avro.UserActionAvro;

public class UserActionDeserializer extends AvroDeserializer<UserActionAvro> {

    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}