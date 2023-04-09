package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.FilmInformationNotExistException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.exception.UserNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film put(Film film) {
        return filmStorage.put(film);
    }

    public Film getFilm(int id) {
        return filmStorage.getFilm(id);
    }

    //тут аналогично с UserService вопрос
    public void setLike(int filmId, int userId) {
        Film film = getFilm(filmId);
        if (film == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        String sql = "INSERT INTO film_likes(film_id, user_id) VALUES(?,?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public void deleteLike(int filmId, int userId) {
        Film film = getFilm(filmId);
        if (film == null) {
            throw new FilmNotExistException("Фильм с id: " + filmId + " не найден.");
        }
        if (userStorage.getUser(userId) == null) {
            throw new UserNotExistException("Не найден пользователь с id:" + userId);
        }
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public List<Film> topLikedFilms(int count) {
        return filmStorage.findAll().stream()
                .sorted(this::compare)
                .limit(count)
                .collect(Collectors.toList());
    }

    private int compare(Film f0, Film f1) {
        return Integer.compare(f1.getLikes(), f0.getLikes());
    }

    //Надеюсь я правильно понял, что жанры и MPA перенести в сервис, но если честно, кажется, что я только хуже сделал
    public FilmGenre getGenre(int genreId) {
        String sql = "SELECT * FROM GENRES where genre_id = ?";
        List<FilmGenre> filmGenre =
                jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs), genreId);
        if (!filmGenre.isEmpty()) {
            return filmGenre.get(0);
        } else {
            throw new FilmInformationNotExistException("Жанра с id: " + genreId + " не существует");
        }
    }

    public Set<FilmGenre> getAllGenres() {
        String sql = "SELECT * FROM GENRES ORDER BY genre_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs)));
    }

    public FilmMpa getMpa(int mpaId) {
        String sql = "SELECT * FROM mpa WHERE mpa_id = ?";
        List<FilmMpa> filmMpa = jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmMpa(rs), mpaId);
        if (!filmMpa.isEmpty()) {
            return filmMpa.get(0);
        } else {
            throw new FilmInformationNotExistException("MPA с id: " + mpaId + " не существует");
        }
    }

    public Set<FilmMpa> getAllMpa() {
        String sql = "SELECT * FROM MPA ORDER BY mpa_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmMpa(rs)));
    }

    private FilmMpa makeFilmMpa(ResultSet rs) throws SQLException {
        return new FilmMpa(rs.getInt("mpa_id"), rs.getString("name"));
    }

    private FilmGenre makeFilmGenre(ResultSet rs) throws SQLException {
        return new FilmGenre(rs.getInt("genre_id"), rs.getString("name"));
    }
}