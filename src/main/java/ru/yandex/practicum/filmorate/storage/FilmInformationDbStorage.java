package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.FilmInformationNotExistException;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class FilmInformationDbStorage implements FilmInformation {
    private final JdbcTemplate jdbcTemplate;

    public FilmInformationDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
