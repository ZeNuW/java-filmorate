package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage, JdbcTemplate jdbcTemplate) {
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
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

    /*
    Не совсем понимаю, почему именно в сервисе нужно делать запрос к БД. Если я правильно понял, сервис это структура
    универсальная, т.е она должна работать без изменений как с реализацией в памяти, так и с реализацией в БД, хоть и
    предполагается, что использоваться будет только реализация с БД. А сейчас она будет работать только с реализацией через БД
    Надеюсь, я правильно понял все недочёты.
    Остальное надеюсь я исправил правильно.
     */
    public List<User> getFriends(int id) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        List<Integer> friendIds = jdbcTemplate.queryForList(sql, Integer.class, id);
        return friendIds.stream().map(this::getUser).collect(Collectors.toList());
    }

    public void addFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        if (getFriends(id).contains(getUser(friendId))) {
            throw new UserDataException("Этот пользователь уже ваш друг");
        }
        String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?,?)";
        jdbcTemplate.update(sql, id, friendId);
        if (getFriends(friendId).contains(getUser(id))) {
            sql = "UPDATE friends SET CONFIRMED_STATUS = true WHERE user_id = ? AND friend_id = ?";
            jdbcTemplate.update(sql, id, friendId);
            jdbcTemplate.update(sql, friendId, id);
        }
    }

    public void deleteFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, id, friendId);
        sql = "UPDATE friends SET CONFIRMED_STATUS = false WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, friendId, id);
    }

    public List<User> getMutualFriends(int id, int friendId) {
        String sql = "SELECT f1.friend_id FROM friends f1 " +
                "INNER JOIN friends f2 ON f1.friend_id = f2.friend_id \n" +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        List<Integer> mutualFriendsId = jdbcTemplate.queryForList(sql, Integer.class,id, friendId);
        return mutualFriendsId.stream().map(this::getUser).collect(Collectors.toList());
    }
}