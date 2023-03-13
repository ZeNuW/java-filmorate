package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserStorage userStorage;
    private final UserService userService;

    @Autowired
    public UserController(UserStorage userStorage, UserService userService) {
        this.userStorage = userStorage;
        this.userService = userService;
    }

    @GetMapping("{id}")
    public User getUser(@PathVariable("id") int id) {
        return userService.getUser(id);
    }

    @GetMapping
    public List<User> findAll() {
        return userStorage.findAll();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return userStorage.create(user);
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        return userStorage.update(user);
    }

    @PutMapping("{id}/friends/{friendId}")
    public void addFriend(@PathVariable("id") int id,
                          @PathVariable("friendId") int friendId) {
        userService.addFriend(id, friendId);
    }

    @DeleteMapping("{id}/friends/{friendId}")
    public void deleteFriend(@PathVariable("id") int id,
                             @PathVariable("friendId") int friendId) {
        userService.deleteFriend(id, friendId);
    }

    @GetMapping("{id}/friends")
    public List<User> listOfFriends(@PathVariable("id") int id) {
        return userService.getFriends(id);
    }

    @GetMapping("{id}/friends/common/{otherId}")
    public List<User> listOfFriends(@PathVariable("id") int id,
                                    @PathVariable("otherId") int otherId) {
        return userService.getMutualFriends(id, otherId);
    }

}
