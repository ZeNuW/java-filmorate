package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.yandex.practicum.filmorate.exception.*;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FilmControllerTest {

    URI url;
    String trueResponse;
    String jsonString;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void findAll() throws Exception {
        // №1
        jsonString =
                "{\"name\":\"nisi eiusmod\",\"description\":\"adipisicing\"" +
                        ",\"releaseDate\":\"1967-03-25\",\"duration\":100}";
        mockMvc.perform(post("/films")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));
        // #2
        jsonString =
                "{\"name\":\"nisi eiusmod 1 more\",\"description\":\"adipisicing added\"" +
                        ",\"releaseDate\":\"1967-03-25\",\"duration\":100}";
        mockMvc.perform(post("/films")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));
        // Проверка списка
        trueResponse = "[{\"id\":1,\"name\":\"nisi eiusmod\",\"description\":\"adipisicing\"" +
                ",\"releaseDate\":\"1967-03-25\",\"duration\":100},{\"id\":2,\"name\":\"nisi eiusmod 1 more\"" +
                ",\"description\":\"adipisicing added\",\"releaseDate\":\"1967-03-25\",\"duration\":100}]";
        mockMvc.perform(get("/films")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));
    }

    @Test
    public void createTest() throws Exception {
        url = URI.create("http://localhost:8080/films");
        // правильный запрос
        trueResponse = "{\"id\":1,\"name\":\"nisi eiusmod\",\"description\":\"adipisicing\"" +
                ",\"releaseDate\":\"1967-03-25\",\"duration\":100}";
        jsonString =
                "{\"name\":\"nisi eiusmod\",\"description\":\"adipisicing\"" +
                        ",\"releaseDate\":\"1967-03-25\",\"duration\":100}";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));

        //запрос с ошибкой в имени
        jsonString =
                "{\"name\":\"\",\"description\":\"Description\",\"releaseDate\":\"1900-03-25\",\"duration\":200}";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof InvalidFilmNameException))
                .andExpect(status().is4xxClientError());

        //запрос с ошибкой в описании
        jsonString =
                "{\"name\":\"Film name\",\"description\":\"Пятеро друзей ( комик-группа «Шарло»)," +
                        " приезжают в город Бризуль. Здесь они хотят разыскать господина Огюста Куглова," +
                        " который задолжал им деньги, а именно 20 миллионов." +
                        " о Куглов, который за время «своего отсутствия», стал кандидатом Коломбани." +
                        "\",\"releaseDate\":\"1900-03-25\",\"duration\":200}";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof FilmDataException))
                .andExpect(status().is4xxClientError());

        //запрос с ошибкой в дате
        jsonString =
                "{\"name\":\"Name\",\"description\":\"Description\",\"releaseDate\":\"1890-03-25\",\"duration\":200}";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof FilmDataException))
                .andExpect(status().is4xxClientError());

        //запрос с ошибкой в продолжительности
        jsonString =
                "{\"name\":\"Name\",\"description\":\"Description\",\"releaseDate\":\"1990-03-25\",\"duration\":-200}";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof FilmDataException))
                .andExpect(status().is4xxClientError());

        //пустой запрос
        jsonString = "";
        mockMvc.perform(post("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof RuntimeException))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void putTest() throws Exception {
        // создание фильма для обновления
        jsonString =
                "{\"name\":\"nisi eiusmod\",\"description\":\"adipisicing\"" +
                        ",\"releaseDate\":\"1967-03-25\",\"duration\":100}";
        mockMvc.perform(post("/films")
                .content(jsonString)
                .contentType(MediaType.APPLICATION_JSON));

        // обновление задачи
        jsonString =
                "{\"id\":1,\"name\":\"Film Updated\",\"releaseDate\":\"1989-04-17\"," +
                        "\"description\":\"New film update decription\",\"duration\":190,\"rate\":4}";
        trueResponse = "{\"id\":1,\"name\":\"Film Updated\",\"description\":\"New film update decription\"," +
                "\"releaseDate\":\"1989-04-17\",\"duration\":190}";
        mockMvc.perform(put("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(trueResponse));

        // некорректное обновление
        jsonString =
                "{\"id\":9999,\"name\":\"Film Updated\",\"releaseDate\":\"1989-04-17\"," +
                        "\"description\":\"New film update decription\",\"duration\":190,\"rate\":4}";
        mockMvc.perform(put("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof FilmNotExistException))
                .andExpect(status().is4xxClientError());
        //пустой запрос
        jsonString = "";
        mockMvc.perform(put("/films")
                        .content(jsonString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(
                        result.getResolvedException() instanceof RuntimeException))
                .andExpect(status().is4xxClientError());
    }
}
