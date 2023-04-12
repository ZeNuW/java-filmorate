package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private int id;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        id = getLastAddedUserId();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeUser(rs));
    }

    @Override
    public User create(User user) {
        if (user.getId() != 0 && getUser(user.getId()) != null) {
            log.warn("Пользователь " + user + " уже существует");
            throw new UserAlreadyExistException("Пользователь с id: "
                    + user.getId() + " уже зарегистрирован.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        //users table
        user.setId(++id);
        String sql = "INSERT INTO users (user_id, name, login, email, birthday) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getId(), user.getName(), user.getLogin(), user.getEmail(), user.getBirthday());
        //friends table
        updateFriends(user);
        return user;
    }

    @Override
    public User update(User user) {
        if (getUser(user.getId()) == null) {
            log.warn("Пользователя " + user + " не существует.");
            throw new UserNotExistException("Данного пользователя не существует.");
        }
        //users table
        String sql = "UPDATE users SET name = ?, login = ?, email = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, user.getName(), user.getLogin(), user.getEmail(), user.getBirthday(), user.getId());
        //friends table
        updateFriends(user);
        return user;
    }

    private void updateFriends(User user) {
        Set<Integer> diffFriends;
        String action;
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        Set<Integer> sqlTableFriends = new HashSet<>(
                jdbcTemplate.queryForList(sql, Integer.class, user.getId()));
        Set<Integer> userFriends = new HashSet<>();
        if (user.getFriends() != null) {
            userFriends.addAll(user.getFriends());
        }
        if (sqlTableFriends.size() < userFriends.size()) {
            sql = "INSERT INTO friends (user_id,friend_id) VALUES (?,?)";
            diffFriends = new HashSet<>(userFriends);
            diffFriends.removeAll(sqlTableFriends);
            action = "INSERT";
        } else if (sqlTableFriends.size() > userFriends.size()) {
            sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
            diffFriends = new HashSet<>(sqlTableFriends);
            diffFriends.removeAll(userFriends);
            action = "DELETE";
        } else {
            return;
        }
        String finalSql = sql;
        diffFriends.forEach(friendId -> jdbcTemplate.update(finalSql, user.getId(), friendId));
        diffFriends.forEach(friendId -> changeFriendshipStatus(user.getId(), friendId, action));
    }

    private void changeFriendshipStatus(int userId, int friendId, String action) {
        //проверяем, что user_id уже является другом friend_id, поэтому в jdbc обратный порядок
        String sql = "SELECT COUNT(*) > 0 FROM friends WHERE user_id = ? AND friend_id = ?";
        boolean isFriend = Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, friendId, userId));
        if (isFriend && action.equals("INSERT")) {
            sql = "UPDATE friends SET confirmed_status = true WHERE user_id = ? AND friend_id = ?";
            //обновляем статусы дружбы обоих пользователей
            jdbcTemplate.update(sql, friendId, userId);
            jdbcTemplate.update(sql, userId, friendId);
        }
        if (isFriend && action.equals("DELETE")) {
            sql = "UPDATE friends SET confirmed_status = false WHERE user_id = ? AND friend_id = ?";
            jdbcTemplate.update(sql, friendId, userId);
        }
    }

    @Override
    public User getUser(int id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> user = jdbcTemplate.query(sql, (rs, rowNum) -> (makeUser(rs)), id);
        if (!user.isEmpty()) {
            return user.get(0);
        } else {
            throw new UserNotExistException("Пользователя с id: " + id + " не существует");
        }
    }

    private User makeUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("login"),
                rs.getString("email"),
                rs.getObject("birthday", LocalDate.class),
                getUserFriends(rs.getInt("user_id")));
    }

    private Set<Integer> getUserFriends(int userId) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, userId));
    }

    private int getLastAddedUserId() {
        String sql = "SELECT MAX(user_id) FROM users";
        Integer userId = jdbcTemplate.queryForObject(sql, Integer.class);
        if (userId != null) {
            return userId;
        }
        return 0;
    }

    @Override
    public List<User> getFriends(int id) {
        String sql = "SELECT * FROM users WHERE user_id IN (SELECT friend_id FROM friends WHERE user_id = " + id + ")";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeUser(rs));
    }

    @Override
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

    @Override
    public void deleteFriend(int id, int friendId) {
        if (id <= 0) {
            throw new UserDataException("Передан отрицательный id " + id);
        }
        if (friendId <= 0) {
            throw new UserDataException("Передан отрицательный id " + friendId);
        }
        if (!getFriends(id).contains(getUser(friendId))) {
            throw new UserDataException("Этот пользователь не является вашим другом");
        }
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, id, friendId);
        sql = "UPDATE friends SET CONFIRMED_STATUS = false WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, friendId, id);
    }

    @Override
    public List<User> getMutualFriends(int id, int friendId) {
        String sql = "SELECT f1.friend_id FROM friends f1 " +
                "INNER JOIN friends f2 ON f1.friend_id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";
        List<Integer> mutualFriendsId = jdbcTemplate.queryForList(sql, Integer.class, id, friendId);
        return mutualFriendsId.stream().map(this::getUser).collect(Collectors.toList());
    }
}