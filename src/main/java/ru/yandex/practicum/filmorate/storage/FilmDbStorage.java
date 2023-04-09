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
        String sql = "SELECT f.*, COUNT(fl.user_id) AS total_likes," +
                "(SELECT fm.name WHERE fm.MPA_ID = f.MPA_ID) AS mpa_name FROM film AS f " +
                "LEFT JOIN film_likes AS fl ON f.film_id = fl.film_id " +
                "LEFT JOIN mpa AS fm ON f.mpa_id = fm.mpa_id " +
                "GROUP BY f.film_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs));
    }

    @Override
    public Film getFilm(int id) {
        String sql = "SELECT f.*, COUNT(fl.user_id) AS total_likes," +
                "(SELECT fm.name WHERE fm.mpa_id = f.mpa_id) AS mpa_name FROM film AS f " +
                "LEFT JOIN film_likes AS fl ON f.film_id = fl.film_id " +
                "LEFT JOIN mpa AS fm ON f.mpa_id = fm.mpa_id " +
                "WHERE f.film_id = ? GROUP BY f.film_id";
        List<Film> film = jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs), id);
        if (!film.isEmpty()) {
            return film.get(0);
        } else {
            throw new FilmNotExistException("Фильма с id: " + id + " не существует");
        }
    }

    /*
    С жанрами если честно я вообще не понял. "Получать список жанров необходимо через сервис. Обрати внимание,
    чтобы при получении всех фильмов, жанры были получены в одном запросе, а не выполнены в цикле перебором по фильмам".
    Я перенёс MPA и жанры в сервис, отсюда доступа я к ним не имею. У меня получилось дублирование кода и я не совсем
    понимаю, как мне сделать 1 запрос к жанрам, а после уже этот список как-то использовать для заполнения жанров всех фильмов.
     */
    private Film makeFilm(ResultSet rs) throws SQLException {
        return new Film(rs.getInt("film_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("release_date", LocalDate.class),
                rs.getInt("duration"),
                rs.getInt("total_likes"),
                getFilmGenres(rs.getInt("film_id")),
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
        //film_genre table
        updateFilmGenre(film);
        log.debug("Обновлён фильм: " + film);
        return getFilm(film.getId());
    }

    private Set<FilmGenre> getFilmGenres(int filmId) {
        String sql = "SELECT genres.genre_id AS genre_id, genres.name AS name FROM genres " +
                "JOIN film_genres ON genres.genre_id = film_genres.genre_id " +
                "WHERE film_genres.film_id = " + filmId + " ORDER BY genres.genre_id";
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs)));
    }

    private FilmGenre makeFilmGenre(ResultSet rs) throws SQLException {
        return new FilmGenre(rs.getInt("genre_id"), rs.getString("name"));
    }

    private void updateFilmGenre(Film film) {
        String sql = "SELECT * FROM genres where genre_id IN (SELECT genre_id FROM film_genres WHERE film_id = ?)";
        Set<Integer> sqlTableGenres = new HashSet<>(
                jdbcTemplate.query(sql, (rs, rowNum) -> makeFilmGenre(rs).getId(), film.getId()));
        Set<Integer> filmGenres = new HashSet<>();
        if (film.getGenres() != null) {
            filmGenres = film.getGenres().stream().map(FilmGenre::getId).collect(Collectors.toSet());
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
                .map(genreId -> new Object[]{film.getId(), genreId})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql, args);
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