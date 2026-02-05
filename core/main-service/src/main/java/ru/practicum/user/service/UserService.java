package ru.practicum.user.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.PageParams;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    UserDto getUserById(Long id);

    List<UserDto> getUsers(List<Long> ids, PageParams pageParams);

    void deleteUser(Long id);

    UserDto updateUser(Long id, UserDto userDto);

}