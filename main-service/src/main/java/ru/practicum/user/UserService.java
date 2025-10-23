package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Создание пользователя: {}", newUserRequest.getEmail());

        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("Пользователь с email '" + newUserRequest.getEmail() + "' уже существует");
        }

        validateUserData(newUserRequest);

        User user = userMapper.toUser(newUserRequest);
        User savedUser = userRepository.save(user);

        log.info("Пользователь создан с ID: {}", savedUser.getId());
        return userMapper.toUserDto(savedUser);
    }

    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        log.info("Получение списка пользователей: ids={}, from={}, size={}", ids, from, size);

        validatePaginationParams(from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size, Sort.by("id").ascending());

        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageRequest).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageRequest).getContent();
        }

        return users.stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeUser(Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с ID {} удален", userId);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
    }

    private void validateUserData(NewUserRequest newUserRequest) {
        if (newUserRequest.getName() == null || newUserRequest.getName().trim().isEmpty()) {
            throw new ConflictException("Имя пользователя не может быть пустым");
        }

        if (newUserRequest.getEmail() == null || newUserRequest.getEmail().trim().isEmpty()) {
            throw new ConflictException("Email не может быть пустым");
        }

        if (!newUserRequest.getEmail().contains("@")) {
            throw new ConflictException("Некорректный формат email");
        }
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (from < 0) {
            throw new IllegalArgumentException("Параметр 'from' не может быть отрицательным");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Параметр 'size' должен быть положительным");
        }
    }
}