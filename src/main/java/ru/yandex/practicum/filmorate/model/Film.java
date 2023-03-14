package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class Film {

    @NotNull(message = "ID должен быть задан.")
    private int id;
    @NotBlank(message = "Имя фильма не может быть пустым.")
    private String name;
    @Size(min = 1, max = 200, message = "Описание фильма может содержать от 1 до 200 символов.")
    private String description;
    @NotNull(message = "Дата релиза не может быть не задана.")
    private LocalDate releaseDate;
    @Positive(message = "Продолжительность фильма не может быть отрицательной или равной 0")
    private int duration;
    private Set<Integer> likes = new HashSet<>();
}
