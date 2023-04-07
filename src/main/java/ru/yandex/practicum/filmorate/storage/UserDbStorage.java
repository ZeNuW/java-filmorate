package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.UserAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        //Тут я сделал так, чтобы всё работало, даже если будет изменено несколько друзей, хотя вообще такого быть не должно
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
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> (makeUser(rs)), id).get(0);
        } catch (IndexOutOfBoundsException e) {
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
}
