package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.InvalidEmailException;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private int id = 0;

    @GetMapping
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User create(@RequestBody User user) {
        if (users.containsKey(user.getId())) {
            log.warn("Пользователь " + user + " уже существует");
            throw new UserAlreadyExistException("Пользователь с id: "
                    + user.getId() + " уже зарегистрирован.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Пустой или некорректный адрес у пользователя " + user);
            throw new InvalidEmailException("Адрес электронной почты не может быть пустым или не содержать знака @.");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Пустой логин у пользователя " + user);
            throw new UserDataException("Логин не может быть пустым или содержать знак пробела.");
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Дата рождения пользователя " + user + " в будущем");
            throw new UserDataException("Дата рождения не может быть в будущем времени.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        user.setId(++id);
        users.put(user.getId(), user);
        log.debug("Добавлен новый пользователь: " + user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) {
        if (users.get(user.getId()) == null) {
            log.warn("Пользователя " + user + " не существует.");
            throw new UserNotExistException("Данного пользователя не существует.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Пустой или некорректный адрес у пользователя " + user);
            throw new InvalidEmailException("Адрес электронной почты не может быть пустым или не содержать знака @.");
        }
        users.put(user.getId(), user);
        log.debug("Добавлен/обновлён пользователь: " + user);
        return user;
    }

}
