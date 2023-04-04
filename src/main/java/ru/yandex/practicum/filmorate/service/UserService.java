package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User getUser(int id) {
        return userStorage.getUser(id);
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User create(User user) {
        return userStorage.create(user);
    }

    public User update(User user) {
        return userStorage.update(user);
    }

    public List<User> getFriends(int id) {
        List<User> friendsList = new ArrayList<>();
        for (Integer userId : userStorage.getUser(id).getFriends()) {
            friendsList.add(userStorage.getUser(userId));
        }
        return friendsList;
    }

    public void addFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        if (userStorage.getUser(id).getFriends().contains(friendId)) {
            throw new UserDataException("Этот пользователь уже ваш друг");
        }
        User user = getUser(id);
        user.getFriends().add(friendId);
        userStorage.update(user);
    }

    public void deleteFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        User user = getUser(id);
        user.getFriends().remove(friendId);
        userStorage.update(user);
    }

    public List<User> getMutualFriends(int id, int friendId) {
        Set<Integer> firstSet = new HashSet<>(userStorage.getUser(id).getFriends());
        Set<Integer> secondSet = new HashSet<>(userStorage.getUser(friendId).getFriends());
        firstSet.retainAll(secondSet);
        List<User> mutualFriendsList = new ArrayList<>();
        for (Integer userId : firstSet) {
            mutualFriendsList.add(userStorage.getUser(userId));
        }
        return mutualFriendsList;
    }
}
