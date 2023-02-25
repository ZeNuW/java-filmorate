package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.LocalDate;

@Data
public class User {

    int id;
    @Email(message = "Email задан некорректно.")
    String email;
    @NotNull(message = "Логин должен быть задан.")
    @NotBlank(message = "Логин не может быть пустым.")
    String login;
    String name;
    @PastOrPresent(message = "Дата рождения не может быть в будущем.")
    LocalDate birthday;

}
