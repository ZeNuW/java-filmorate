package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.FilmGenre;
import ru.yandex.practicum.filmorate.model.FilmMpa;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Set;

@Slf4j
@RestController
public class FilmInformationController {

    private final FilmService filmService;

    @Autowired
    public FilmInformationController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping("/genres")
    public Set<FilmGenre> getAllGenres() {
        return filmService.getAllGenres();
    }

    @GetMapping("/genres/{genre_id}")
    public FilmGenre getGenre(@PathVariable int genre_id) {
        return filmService.getGenre(genre_id);
    }

    @GetMapping("/mpa")
    public Set<FilmMpa> getAllMpa() {
        return filmService.getAllMpa();
    }

    @GetMapping("/mpa/{mpa_id}")
    public FilmMpa getMpa(@PathVariable int mpa_id) {
        return filmService.getMpa(mpa_id);
    }
}
