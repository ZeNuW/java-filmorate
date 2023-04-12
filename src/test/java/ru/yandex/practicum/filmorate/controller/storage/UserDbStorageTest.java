package ru.yandex.practicum.filmorate.controller.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserDbStorageTest {
    private final UserDbStorage userStorage;

    @BeforeAll
    public static void beforeAll(@Autowired UserDbStorage userStorage) {
        //add 2 users
        User user = new User(0, "Nick Name", "dolore", "mail@mail.ru",
                LocalDate.of(1946, 8, 20), new HashSet<>());
        userStorage.create(user);
        User user2 = new User(0, "friend adipisicing", "friend", "friend@mail.ru",
                LocalDate.of(1976, 8, 20), new HashSet<>());
        userStorage.create(user2);
    }

    @Test
    public void testGetUser() {
        assertThat(userStorage.getUser(1))
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("name", "Nick Name")
                .hasFieldOrPropertyWithValue("login", "dolore")
                .hasFieldOrPropertyWithValue("birthday", LocalDate.of(1946, 8, 20))
                .hasFieldOrPropertyWithValue("email", "mail@mail.ru");
        assertThat(userStorage.getUser(2))
                .hasFieldOrPropertyWithValue("id", 2)
                .hasFieldOrPropertyWithValue("name", "friend adipisicing")
                .hasFieldOrPropertyWithValue("login", "friend")
                .hasFieldOrPropertyWithValue("birthday", LocalDate.of(1976, 8, 20))
                .hasFieldOrPropertyWithValue("email", "friend@mail.ru");
    }

    @Test
    public void testFindAllUsers() {
        assertThat(userStorage.findAll())
                .contains(userStorage.getUser(1), userStorage.getUser(2));
    }

    @Test
    @Transactional
    public void testCreateUser() {
        //ok
        User user = new User(0, "Test Name", "test", "test@mail.ru",
                LocalDate.of(2000, 1, 1), new HashSet<>());
        userStorage.create(user);
        assertThat(userStorage.getUser(3))
                .hasFieldOrPropertyWithValue("id", 3)
                .hasFieldOrPropertyWithValue("name", "Test Name")
                .hasFieldOrPropertyWithValue("login", "test")
                .hasFieldOrPropertyWithValue("birthday", LocalDate.of(2000, 1, 1))
                .hasFieldOrPropertyWithValue("email", "test@mail.ru");
        //already added fail
        assertThrows(UserAlreadyExistException.class, () -> userStorage.create(user));
    }

    @Test
    @Transactional
    public void testPutUser() {
        //ok
        User user = new User(1, "Updated Name", "dolore", "mail_updated@mail.ru",
                LocalDate.of(1946, 8, 20), new HashSet<>());
        userStorage.update(user);
        assertEquals(user, userStorage.getUser(1), "Фильм не был обновлён");
        //fail
        user.setId(99);
        assertThrows(UserNotExistException.class, () -> userStorage.update(user));
    }
}