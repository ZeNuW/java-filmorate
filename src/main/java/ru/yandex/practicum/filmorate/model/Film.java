package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class Film {

    int id;
    @NotNull(message = "Имя фильма должно быть задано.")
    @NotBlank(message = "Имя фильма не может быть пустым")
    String name;
    @Size(min = 1, max = 200, message = "Описание фильма может содержать от 1 до 200 символов.")
    String description;
    LocalDate releaseDate;
    @Positive(message = "Продолжительность фильма не может быть отрицательной или равной 0")
    int duration;

}
