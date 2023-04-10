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
                "(SELECT fm.name WHERE fm.MPA_ID = f.MPA_ID) AS mpa_name, " +
                "GROUP_CONCAT(CONCAT(g.genre_id, ':', g.name)) AS all_genres " +
                "FROM film AS f " +
                "LEFT JOIN film_likes AS fl ON f.film_id = fl.film_id " +
                "LEFT JOIN mpa AS fm ON f.mpa_id = fm.mpa_id " +
                "LEFT JOIN film_genres AS fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres AS g ON fg.genre_id = g.genre_id " +
                "GROUP BY f.film_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs));
    }

    @Override
    public Film getFilm(int id) {
        String sql = "SELECT f.*, COUNT(fl.user_id) AS total_likes," +
                "(SELECT fm.name WHERE fm.mpa_id = f.mpa_id) AS mpa_name, " +
                "GROUP_CONCAT(CONCAT(g.genre_id, ':', g.name)) AS all_genres " +
                "FROM film AS f " +
                "LEFT JOIN film_likes AS fl ON f.film_id = fl.film_id " +
                "LEFT JOIN mpa AS fm ON f.mpa_id = fm.mpa_id " +
                "LEFT JOIN film_genres AS fg ON f.film_id = fg.film_id " +
                "LEFT JOIN genres AS g ON fg.genre_id = g.genre_id " +
                "WHERE f.film_id = ? GROUP BY f.film_id";
        List<Film> film = jdbcTemplate.query(sql, (rs, rowNum) -> makeFilm(rs), id);
        if (!film.isEmpty()) {
            return film.get(0);
        } else {
            throw new FilmNotExistException("Фильма с id: " + id + " не существует");
        }
    }

    /*
    С жанрами если честно я недопонял. "Получать список жанров необходимо через сервис. Обрати внимание,
    чтобы при получении всех фильмов, жанры были получены в одном запросе, а не выполнены в цикле перебором по фильмам".
    Я перенёс MPA и жанры в сервис. Вроде получилось сделать 1 запросом. Я правильно понял замечание?
     */
    private Film makeFilm(ResultSet rs) throws SQLException {
        String genres = (rs.getString("all_genres") != null) ? rs.getString("all_genres") : ":";
        return new Film(rs.getInt("film_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("release_date", LocalDate.class),
                rs.getInt("duration"),
                rs.getInt("total_likes"),
                getFilmGenres(genres),
                new FilmMpa(rs.getInt("mpa_id"), rs.getString("mpa_name")));
    }

    private Set<FilmGenre> getFilmGenres(String genres) {
        if (genres.equals(":")) {
            return new HashSet<>();
        }
        return Arrays.stream(genres.split(","))
                .map(genre -> {
                    String[] parts = genre.split(":");
                    return new FilmGenre(Integer.parseInt(parts[0]), parts[1]);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        removeFrom.removeAll(removableElements);
        if (removeFrom.isEmpty()) {
            return;
        }
        List<Object[]> args = removeFrom.stream()
                .map(genreId -> new Object[]{filmId, genreId})
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