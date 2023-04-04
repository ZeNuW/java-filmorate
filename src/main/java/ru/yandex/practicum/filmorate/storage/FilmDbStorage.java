package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmAlreadyExistException;
import ru.yandex.practicum.filmorate.exception.FilmDataException;
import ru.yandex.practicum.filmorate.exception.FilmInformationNotExistException;
import ru.yandex.practicum.filmorate.exception.FilmNotExistException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
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
        String sql = "SELECT * FROM film";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs));
    }

    @Override
    public Film getFilm(int id) {
        String sql = "SELECT * FROM film WHERE film_id = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs), id).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new FilmNotExistException("Фильма с id " + id + " не существует.");
        }
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
        //Не стал добавлять таблицу лайков, т.к вроде логично, что при создании фильма никаких лайков у него нет.
        //Film_genre table
        updateFilmGenre(film);
        return getFilm(film.getId());
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
        //film_likes table
        updateFilmLikes(film);
        //film_genre table
        updateFilmGenre(film);
        log.debug("Обновлён фильм: " + film);
        return getFilm(film.getId());
    }

    @Override
    public FilmGenre getGenre(int genre_id) {
        String sql = "SELECT * FROM GENRES where genre_id = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs), genre_id).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new FilmInformationNotExistException("Жанра с id " + genre_id + " не существует.");
        }
    }

    @Override
    public Set<FilmGenre> getAllGenres() {
        String sql = "SELECT * FROM GENRES ORDER BY genre_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs)));
    }

    @Override
    public FilmMpa getMpa(int mpa_id) {
        String sql = "SELECT * FROM MPA where mpa_id = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmMpa(rs), mpa_id).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new FilmInformationNotExistException("MPA с id " + mpa_id + " не существует.");
        }
    }

    @Override
    public Set<FilmMpa> getAllMpa() {
        String sql = "SELECT * FROM MPA ORDER BY mpa_id";
    return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmMpa(rs)));
    }

    private Film makeFilm(ResultSet rs) throws SQLException {
        return new Film(rs.getInt("film_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("release_date", LocalDate.class),
                rs.getInt("duration"),
                getFilmLikes(rs.getInt("film_id")),
                getFilmGenres(rs.getInt("film_id")),
                getFilmMpa(rs.getInt("mpa_id")));
    }

    private Set<Integer> getFilmLikes(int filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, filmId));
    }

    private Set<FilmGenre> getFilmGenres(int filmId) {
        String sql = "SELECT * FROM genres " +
                "WHERE genre_id IN (SELECT genre_id FROM film_genres WHERE film_id = ?) ORDER BY genre_id";
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs), filmId));
    }

    private FilmGenre makeFilmGenre(ResultSet rs) throws SQLException {
        return new FilmGenre(rs.getInt("genre_id"), rs.getString("name"));
    }


    private FilmMpa getFilmMpa(int mpa_id) {
        String sql = "SELECT * FROM mpa WHERE mpa_id = ?";
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmMpa(rs), mpa_id).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new FilmDataException("MPA с id " + mpa_id + " не существует.");
        }
    }

    private FilmMpa makeFilmMpa(ResultSet rs) throws SQLException {
        return new FilmMpa(rs.getInt("mpa_id"), rs.getString("name"));
    }

    private int getLastAddedFilmId() {
        String sql = "SELECT MAX(film_id) FROM film";
        Integer lastId = jdbcTemplate.queryForObject(sql, Integer.class);
        if (lastId != null) {
            return lastId;
        }
        return 0;
    }

    private void updateFilmLikes(Film film) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        Set<Integer> sqlTableLikes = new HashSet<>(
                jdbcTemplate.queryForList(sql, Integer.class, film.getId()));
        Set<Integer> filmLikes;
        if (film.getLikes() != null) {
            filmLikes = new HashSet<>(film.getLikes());
        } else {
            filmLikes = new HashSet<>();
        }
        Set<Integer> likes;
        if (sqlTableLikes.size() < filmLikes.size()) {
            sql = "INSERT INTO film_likes (film_id,user_id) VALUES (?,?)";
            likes = new HashSet<>(filmLikes);
            likes.removeAll(sqlTableLikes);
        } else if (sqlTableLikes.size() > filmLikes.size()) {
            sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
            likes = new HashSet<>(sqlTableLikes);
            likes.removeAll(filmLikes);
        } else {
            return;
        }
        List<Object[]> args = likes.stream()
                .map(user_id -> new Object[]{film.getId(), user_id})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql, args);
    }

    private void updateFilmGenre(Film film) {
        String sql = "SELECT * FROM genres where genre_id IN (SELECT genre_id FROM film_genres WHERE film_id = ?)";
        Set<Integer> sqlTableGenres = new HashSet<>(
                jdbcTemplate.query(sql,(rs,rowNum) -> makeFilmGenre(rs).getId(),film.getId()));
        Set<Integer> filmGenres;
        if (film.getGenres() != null) {
            filmGenres = film.getGenres().stream().map(FilmGenre::getId).collect(Collectors.toSet());
        } else {
            filmGenres = new HashSet<>();
        }
        Set<Integer> genres;
        if (sqlTableGenres.size() < filmGenres.size()) {
            sql = "INSERT INTO film_genres (film_id,genre_id) VALUES (?,?)";
            genres = new HashSet<>(filmGenres);
            genres.removeAll(sqlTableGenres);
        } else if (sqlTableGenres.size() > filmGenres.size()) {
            sql = "DELETE FROM film_genres WHERE film_id = ? AND genre_id = ?";
            genres = new HashSet<>(sqlTableGenres);
            genres.removeAll(filmGenres);
        } else {
            return;
        }
        List<Object[]> args = genres.stream()
                .map(genre_id -> new Object[]{film.getId(), genre_id})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql, args);
    }
}