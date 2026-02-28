package ru.practicum.mapper;

import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;


@Mapper(componentModel = "spring")
public interface UserActionMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "actionType", source = "actionType", qualifiedByName = "mapActionType")
    @Mapping(target = "timestamp", source = "timestamp", qualifiedByName = "mapTimestamp")
    UserActionAvro mapToAvro(UserActionProto proto);

    @Named("mapTimestamp")
    default long mapToTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return 0L;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).toEpochMilli();
    }

    @Named("mapActionType")
    default ActionTypeAvro mapActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Неизвестный тип действия: " + protoType);
        };
    }
}