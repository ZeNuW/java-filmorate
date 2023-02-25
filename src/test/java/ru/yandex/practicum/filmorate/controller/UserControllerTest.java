package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.practicum.filmorate.exception.InvalidEmailException;
import ru.yandex.practicum.filmorate.exception.UserDataException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerTest {

    URI url;
    String trueResponse;
    String jsonString;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void findAll() throws Exception {
        // №1
        jsonString =
                "{\"login\":\"dolore\",\"name\":\"Nick Name\",\"email\":\"mail@mail.ru\",\"birthday\":\"1946-08-20\"}";
        mockMvc.perform(post("/users")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));
        // #2
        jsonString =
                "{\"login\":\"doloreNew\",\"name\":\"Nick Name\",\"email\":\"mail2@mail.ru\",\"birthday\":\"1946-08-20\"}";
        mockMvc.perform(post("/users")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));
        // Проверка списка
        trueResponse = "[{\"id\":1,\"email\":\"mail@mail.ru\",\"login\":\"dolore\",\"name\":\"Nick Name\"," +
                "\"birthday\":\"1946-08-20\"},{\"id\":2,\"email\":\"mail2@mail.ru\",\"login\":\"doloreNew\"," +
                "\"name\":\"Nick Name\",\"birthday\":\"1946-08-20\"}]";
        mockMvc.perform(get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));
    }

    @Test
    public void createTest() throws Exception {
        url = URI.create("http://localhost:8080/users");
        // правильный запрос
        trueResponse = "{\"id\":1,\"email\":\"mail@mail.ru\",\"login\":\"dolore\"" +
                ",\"name\":\"Nick Name\",\"birthday\":\"1946-08-20\"}";
        jsonString =
                "{\"login\":\"dolore\",\"name\":\"Nick Name\",\"email\":\"mail@mail.ru\",\"birthday\":\"1946-08-20\"}";
        mockMvc.perform(post("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));

        //запрос с ошибкой в логине
        jsonString =
                "{\"login\":\"dolore ullamco\",\"email\":\"yandex@mail.ru\",\"birthday\":\"2446-08-20\"}";
        mockMvc.perform(post("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof UserDataException))
                .andExpect(status().is4xxClientError());

        //запрос с ошибкой в email
        jsonString =
                "{\"login\":\"dolore ullamco\",\"name\":\"\",\"email\":\"mail.ru\",\"birthday\":\"1980-08-20\"}";
        mockMvc.perform(post("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof InvalidEmailException))
                .andExpect(status().is4xxClientError());

        //запрос с ошибкой в дате
        jsonString =
                "{\"login\":\"dolore\",\"name\":\"\",\"email\":\"test@mail.ru\",\"birthday\":\"2446-08-20\"}";
        mockMvc.perform(post("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof UserDataException))
                .andExpect(status().is4xxClientError());

        //пустой запрос
        jsonString = "";
        mockMvc.perform(post("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof RuntimeException))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void putTest() throws Exception {
        // создание задачи для обновления
        jsonString =
                "{\"login\":\"dolore\",\"name\":\"Nick Name\",\"email\":\"mail@mail.ru\",\"birthday\":\"1946-08-20\"}";
        mockMvc.perform(post("/users")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));

        // обновление задачи
        jsonString =
                "{\"login\":\"doloreUpdate\",\"name\":\"est adipisicing\"," +
                        "\"id\":1,\"email\":\"mail@yandex.ru\",\"birthday\":\"1976-09-20\"}";
        trueResponse = "{\"id\":1,\"email\":\"mail@yandex.ru\",\"login\":\"doloreUpdate\"," +
                "\"name\":\"est adipisicing\",\"birthday\":\"1976-09-20\"}";
        mockMvc.perform(put("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));

        // некорректное обновление
        jsonString =
                "{\"login\":\"doloreUpdate\",\"name\":\"est adipisicing\",\"id\":9999," +
                        "\"email\":\"mail@yandex.ru\",\"birthday\":\"1976-09-20\"}";
        mockMvc.perform(put("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof UserNotExistException))
                .andExpect(status().is4xxClientError());
        //пустой запрос
        jsonString = "";
        mockMvc.perform(put("/users")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof RuntimeException))
                .andExpect(status().is4xxClientError());
    }
}
