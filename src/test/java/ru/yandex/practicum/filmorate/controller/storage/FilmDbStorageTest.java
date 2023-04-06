package ru.yandex.practicum.filmorate.controller.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmDataException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private Set<FilmGenre> filmGenres;

    @BeforeAll
    public static void beforeAll(@Autowired FilmDbStorage filmStorage) {
        //add 2 films
        Set<FilmGenre> filmGenres = new HashSet<>();
        Film film = new Film(0, "nisi eiusmod", "adipisicing",
                LocalDate.of(1967, 3, 25), 100, new HashSet<>(),
                filmGenres, new FilmMpa(1, "G"));
        filmStorage.create(film);
        filmGenres.add(new FilmGenre(1, "Драма"));
        Film film2 = new Film(0, "New film", "New film about friends",
                LocalDate.of(1999, 4, 30), 120, new HashSet<>(),
                filmGenres, new FilmMpa(3, "PG-13"));
        filmStorage.create(film2);
    }

    @Test
    public void testFindFilmById() {
        filmGenres = new LinkedHashSet<>();
        //1
        assertThat(filmStorage.getFilm(1))
                .hasFieldOrPropertyWithValue("id", 1)
                .hasFieldOrPropertyWithValue("name", "nisi eiusmod")
                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(1967, 3, 25))
                .hasFieldOrPropertyWithValue("description", "adipisicing")
                .hasFieldOrPropertyWithValue("duration", 100)
                .hasFieldOrPropertyWithValue("mpa", new FilmMpa(1, "G"))
                .hasFieldOrPropertyWithValue("genres", filmGenres);
        //2
        filmGenres.add(new FilmGenre(1, "Комедия"));
        assertThat(filmStorage.getFilm(2))
                .hasFieldOrPropertyWithValue("id", 2)
                .hasFieldOrPropertyWithValue("name", "New film")
                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(1999, 4, 30))
                .hasFieldOrPropertyWithValue("description", "New film about friends")
                .hasFieldOrPropertyWithValue("duration", 120)
                .hasFieldOrPropertyWithValue("mpa", new FilmMpa(3, "PG-13"))
                .hasFieldOrPropertyWithValue("genres", filmGenres);
        //ошибка
        assertThrows(FilmNotExistException.class, () -> filmStorage.getFilm(99));
    }

    @Test
    @Transactional
    public void testFindAllFilms() {
        assertThat(filmStorage.findAll())
                .contains(filmStorage.getFilm(1), filmStorage.getFilm(2));
    }

    @Test
    @Transactional
    public void testCreateFilm() {
        filmGenres = new LinkedHashSet<>();
        //ok
        filmGenres.add(new FilmGenre(1, "Комедия"));
        filmGenres.add(new FilmGenre(2, "Драма"));
        Film film = new Film(0, "Test film 3", "New test film",
                LocalDate.of(2000, 1, 1), 300, new HashSet<>(), filmGenres,
                new FilmMpa(5, "NC-17"));
        filmStorage.create(film);
        assertThat(filmStorage.getFilm(3))
                .hasFieldOrPropertyWithValue("id", 3)
                .hasFieldOrPropertyWithValue("name", "Test film 3")
                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2000, 1, 1))
                .hasFieldOrPropertyWithValue("description", "New test film")
                .hasFieldOrPropertyWithValue("duration", 300)
                .hasFieldOrPropertyWithValue("mpa", new FilmMpa(5, "NC-17"))
                .hasFieldOrPropertyWithValue("genres", filmGenres);
        //already added fail
        assertThrows(FilmAlreadyExistException.class, () -> filmStorage.create(film));
        //release_date fail
        film.setId(0);
        film.setReleaseDate(LocalDate.of(1800, 1, 1));
        assertThrows(FilmDataException.class, () -> filmStorage.create(film));
    }

    @Test
    @Transactional
    public void testPutFilm() {
        filmGenres = new LinkedHashSet<>();
        //ok
        Film film = new Film(1, "Film Updated", "New test film",
                LocalDate.of(1984, 4, 17), 190, new HashSet<>(), filmGenres,
                new FilmMpa(2, "PG"));
        filmStorage.put(film);
        assertEquals(film, filmStorage.getFilm(1), "Фильм не был обновлён");
        //fail
        film.setId(99);
        assertThrows(FilmNotExistException.class, () -> filmStorage.put(film));
    }

    @Test
    public void testGetGenre() {
        assertEquals(new FilmGenre(1, "Комедия"),
                filmStorage.getGenre(1), "Жанры отличаются");
        assertEquals(new FilmGenre(2, "Драма"),
                filmStorage.getGenre(2), "Жанры отличаются");
        assertEquals(new FilmGenre(3, "Мультфильм"),
                filmStorage.getGenre(3), "Жанры отличаются");
        assertEquals(new FilmGenre(4, "Триллер"),
                filmStorage.getGenre(4), "Жанры отличаются");
        assertEquals(new FilmGenre(5, "Документальный"),
                filmStorage.getGenre(5), "Жанры отличаются");
    }

    @Test
    @Transactional
    public void testGetAllGenres() {
        filmGenres = new LinkedHashSet<>();
        filmGenres.add(new FilmGenre(1, "Комедия"));
        filmGenres.add(new FilmGenre(2, "Драма"));
        filmGenres.add(new FilmGenre(3, "Мультфильм"));
        filmGenres.add(new FilmGenre(4, "Триллер"));
        filmGenres.add(new FilmGenre(5, "Документальный"));
        filmGenres.add(new FilmGenre(6, "Боевик"));
        assertEquals(filmGenres, filmStorage.getAllGenres(), "Списки жанров отличаются");
    }

    @Test
    public void testGetMpa() {
        assertEquals(new FilmMpa(1, "G"),
                filmStorage.getMpa(1), "MPA отличаются");
        assertEquals(new FilmMpa(2, "PG"),
                filmStorage.getMpa(2), "MPA отличаются");
        assertEquals(new FilmMpa(3, "PG-13"),
                filmStorage.getMpa(3), "MPA отличаются");
        assertEquals(new FilmMpa(4, "R"),
                filmStorage.getMpa(4), "MPA отличаются");
        assertEquals(new FilmMpa(5, "NC-17"),
                filmStorage.getMpa(5), "MPA отличаются");
    }

    @Test
    @Transactional
    public void testGetAllMpa() {
        Set<FilmMpa> filmMpa = new LinkedHashSet<>();
        filmMpa.add(new FilmMpa(1, "G"));
        filmMpa.add(new FilmMpa(2, "PG"));
        filmMpa.add(new FilmMpa(3, "PG-13"));
        filmMpa.add(new FilmMpa(4, "R"));
        filmMpa.add(new FilmMpa(5, "NC-17"));
        assertEquals(filmMpa, filmStorage.getAllMpa(), "Списки MPA отличаются");
    }
}