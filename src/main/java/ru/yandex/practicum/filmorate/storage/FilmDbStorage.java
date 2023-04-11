package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmDataException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private static final LocalDate FIRST_FILM_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private int id;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        id = getLastAddedFilmId();
    }

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.mpa_id AS mpa_id, m.name AS mpa_name FROM film f, MPA m " +
                "WHERE f.mpa_id = m.mpa_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs));
    }

    @Override
    public Film getFilm(int id) {
        String sql = "SELECT f.*, m.mpa_id AS mpa_id, m.name AS mpa_name FROM film f, MPA m " +
                "WHERE f.mpa_id = m.mpa_id AND film_id = ?";
        List<Film> film = jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs), id);
        if (!film.isEmpty()) {
            return film.get(0);
        } else {
            throw new FilmNotExistException("Фильма с id: " + id + " не существует");
        }
    }

    private Film makeFilm(ResultSet rs) throws SQLException {
        return new Film(rs.getInt("film_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("release_date", LocalDate.class),
                rs.getInt("duration"),
                rs.getInt("likes"),
                new LinkedHashSet<>(),
                new FilmMpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
    }

    @Override
    public Film create(Film film) {
        if (film.getId() != 0 && getFilm(film.getId()) != null) {
            log.warn("Передан фильм который уже был добавлен " + film);
            throw new FilmAlreadyExistException("Фильм с названием " + film.getName() + " уже есть в списке фильмов.");
        }
        if (film.getReleaseDate().isBefore(FIRST_FILM_RELEASE_DATE)) {
            log.warn("Передан фильм с невозможной датой выхода " + film);
            throw new FilmDataException("Дата релиза фильма не может быть раньше 28 декабря 1895 года.");
        }
        film.setId(++id);
        //film table
        String sql = "INSERT INTO film (film_id, name, description, release_date, duration, mpa_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, film.getId(), film.getName(),
                film.getDescription(), film.getReleaseDate(), film.getDuration(), film.getMpa().getId());
        //Film_genre table
        updateFilmGenre(film);
        log.debug("Добавлен фильм: " + film);
        return film;
    }

    @Override
    public Film put(Film film) {
        if (getFilm(film.getId()) == null) {
            log.warn("Фильма " + film + " нет в списке.");
            throw new FilmNotExistException("Данного фильма не существует.");
        }
        //film table
        String sql = "UPDATE film SET name = ?, description = ?, release_date = ?, duration = ?, " +
                "mpa_id = ? WHERE film_id = ?";
        jdbcTemplate.update(sql, film.getName(),
                film.getDescription(), film.getReleaseDate(), film.getDuration(), film.getMpa().getId(), film.getId());
        //film_genre table
        updateFilmGenre(film);
        log.debug("Обновлён фильм: " + film);
        return film;
    }

    private void updateFilmGenre(Film film) {
        String sql = "SELECT genre_id FROM genres WHERE genre_id IN (SELECT genre_id FROM film_genres WHERE film_id = ?)";
        Set<Integer> sqlTableGenres = new HashSet<>(
                jdbcTemplate.queryForList(sql, Integer.class, film.getId()));
        Set<Integer> filmGenres = new HashSet<>();
        if (film.getGenres() != null) {
            filmGenres = film.getGenres().stream().map(FilmGenre::getId).collect(Collectors.toSet());
        }
        if (!filmGenres.isEmpty()) { //если жанры объекта film не пустые, добавляем тех, что нет в таблице
            sql = "INSERT INTO film_genres (film_id,genre_id) VALUES (?,?)";
            updateGenreTable(sql, filmGenres, sqlTableGenres, film.getId());
        }
        if (!sqlTableGenres.isEmpty()) { //если жанры таблицы фильма не пустые, удаляем те, которых нет в объекте film
            sql = "DELETE FROM film_genres WHERE film_id = ? AND genre_id = ?";
            updateGenreTable(sql, sqlTableGenres, filmGenres, film.getId());
        }
    }

    private void updateGenreTable(String sql, Set<Integer> removeFrom, Set<Integer> removableElements, int filmId) {
        Set<Integer> diff = new HashSet<>(removeFrom);
        diff.removeAll(removableElements);
        if (diff.isEmpty()) {
            return;
        }
        List<Object[]> args = diff.stream()
                .map(genreId -> new Object[]{filmId, genreId})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql, args);
    }

    @Override
    public void setLike(int filmId, int userId) {
        String sql = "INSERT INTO film_likes(film_id, user_id) VALUES(?,?)";
        jdbcTemplate.update(sql, filmId, userId);
        updateFilmLikes(filmId);
    }

    @Override
    public void deleteLike(int filmId, int userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
        updateFilmLikes(filmId);
    }

    private void updateFilmLikes(int filmId) {
        String sql = "UPDATE film f SET likes = " +
                "(SELECT COUNT(fl.user_id) FROM film_likes fl WHERE fl.film_id = f.film_id) WHERE film_id = ?";
        jdbcTemplate.update(sql, filmId);
    }

    @Override
    public List<Film> topLikedFilms(int count) {
        String sql = "SELECT f.*, m.mpa_id AS mpa_id, m.name AS mpa_name FROM film f, MPA m " +
                "WHERE f.mpa_id = m.mpa_id ORDER BY f.likes DESC LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs), count);
    }

    @Override
    public void loadGenre(List<Film> films) {
        /*
        Не придумал ничего проще, наверное это можно сделать.
        Почему-то H2 не захотел принимать массив или список в "?", пришлось делать столько же ?, сколько и фильмов.
        Если ставил fg.film_id IN (?), при использовании массива, он думал, что это отдельные аргументы, а списки
        вообще не воспринимал.
         */
        Map<Integer, Film> filmsMap = films.stream().collect(
                Collectors.toMap(Film::getId, Function.identity()));
        String sql = "SELECT fg.genre_id AS genre_id, g.name AS name, fg.film_id as film_id FROM film_genres AS fg " +
                "LEFT JOIN genres AS g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + String.join(",", Collections.nCopies(films.size(), "?")) + ")";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, filmsMap.keySet().toArray());
        rows.forEach(filmMap -> {
            int filmId = Integer.parseInt(String.valueOf(filmMap.get("film_id")));
            filmsMap.get(filmId).getGenres().add(
                    new FilmGenre(Integer.parseInt(String.valueOf(filmMap.get("genre_id"))),
                            String.valueOf(filmMap.get("name"))));
        });
    }

    private int getLastAddedFilmId() {
        String sql = "SELECT MAX(film_id) FROM film";
        Integer lastId = jdbcTemplate.queryForObject(sql, Integer.class);
        if (lastId != null) {
            return lastId;
        }
        return 0;
    }
}