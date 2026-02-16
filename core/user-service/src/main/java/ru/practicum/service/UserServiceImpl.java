package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.common.EntityValidator;
import ru.practicum.dto.user.UserBatchDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ValidationException;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.PageParams;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EntityValidator entityValidator;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        validateNewUserRequest(request);
        ensureEmailUnique(request.getEmail(), null);

        User user = userMapper.toEntity(request);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = entityValidator.ensureAndGet(userRepository, id, "Пользователь");
        return userMapper.toDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, PageParams pageParams) {
        PageRequest pageable = PageRequest.of(pageParams.getPageNumber(), Math.max(1, pageParams.getSize()), Sort.by("id").ascending());
        List<User> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllByIdIn(ids, pageable);
        } else {
            users = userRepository.findAllBy(pageable);
        }
        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        entityValidator.ensureExists(userRepository, id, "Пользователь");
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = entityValidator.ensureAndGet(userRepository, id, "Пользователь");

        if (userDto.getEmail() != null && !userDto.getEmail().isBlank()) {
            ensureEmailUnique(userDto.getEmail(), id);
        }

        userMapper.updateEntityFromDto(userDto, user);

        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    private void validateNewUserRequest(NewUserRequest req) {
        if (req == null) {
            throw new ValidationException("Запрос на добавление пользователя не должен быть пустым");
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
    }

    private void ensureEmailUnique(String email, Long existingUserIdToIgnore) {
        userRepository.findByEmail(email).ifPresent(u -> {
            if (existingUserIdToIgnore == null || !Objects.equals(u.getId(), existingUserIdToIgnore)) {
                throw new ValidationException("Email должен быть уникальным!");
            }
        });
    }

    @Override
    public UserBatchDto getUsersByIds(List<Long> userIds) {
        UserBatchDto result = new UserBatchDto();

        if (userIds == null || userIds.isEmpty()) {
            result.setUsers(new HashMap<>());
            return result;
        }

        List<User> users = userRepository.findAllById(userIds);

        Map<Long, UserShortDto> userMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> {
                            UserShortDto dto = new UserShortDto();
                            dto.setId(user.getId());
                            dto.setName(user.getName());
                            return dto;
                        }
                ));

        result.setUsers(userMap);
        return result;
    }
}