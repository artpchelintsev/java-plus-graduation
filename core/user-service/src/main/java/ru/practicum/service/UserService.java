package ru.practicum.service;

import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.PageParams;
import ru.practicum.dto.user.UserBatchDto;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    UserDto getUserById(Long id);

    List<UserDto> getUsers(List<Long> ids, PageParams pageParams);

    void deleteUser(Long id);

    UserDto updateUser(Long id, UserDto userDto);

    UserBatchDto getUsersByIds(List<Long> userIds);

}