package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private int id = 0;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User getUser(int id) {
        if (!users.containsKey(id)) {
            throw new UserDataException("Пользователя с id " + id + " не существует");
        }
        return users.get(id);
    }

    @Override
    public User create(User user) {
        if (users.containsKey(user.getId())) {
            log.warn("Пользователь " + user + " уже существует");
            throw new UserAlreadyExistException("Пользователь с id: "
                    + user.getId() + " уже зарегистрирован.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        user.setId(++id);
        users.put(user.getId(), user);
        log.debug("Добавлен новый пользователь: " + user);
        return user;
    }

    @Override
    public User update(User user) {
        if (users.get(user.getId()) == null) {
            log.warn("Пользователя " + user + " не существует.");
            throw new UserNotExistException("Данного пользователя не существует.");
        }
        users.put(user.getId(), user);
        log.debug("Добавлен/обновлён пользователь: " + user);
        return user;
    }
}