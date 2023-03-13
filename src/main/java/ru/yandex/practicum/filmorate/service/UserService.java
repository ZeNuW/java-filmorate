package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    /*
    Не написал тесты так как в тз вроде не просят, но если требуется, то я допишу
     */

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User getUser(int id) {
        User user = userStorage.getUsers().get(id);
        if (user == null) {
            throw new UserNotExistException("Пользователя с id " + id + " не существует");
        }
        return user;
    }

    public List<User> getFriends(int id) {
        List<User> friendsList = new ArrayList<>();
        for (Integer userId : getUser(id).getFriends()) {
            friendsList.add(getUser(userId));
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
        if (getUser(id).getFriends().contains(friendId)) {
            throw new UserDataException("Этот пользователь уже ваш друг");
        }
        getUser(id).getFriends().add(friendId);
        getUser(friendId).getFriends().add(id);
    }

    public void deleteFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        getUser(id).getFriends().remove(friendId);
        getUser(friendId).getFriends().remove(id);
    }

    public List<User> getMutualFriends(int id, int friendId) {
        Set<Integer> firstSet = new HashSet<>(getUser(id).getFriends());
        Set<Integer> secondSet = new HashSet<>(getUser(friendId).getFriends());
        firstSet.retainAll(secondSet);
        List<User> mutualFriendsList = new ArrayList<>();
        for (Integer userId : firstSet) {
            mutualFriendsList.add(getUser(userId));
        }
        return mutualFriendsList;
    }

}
