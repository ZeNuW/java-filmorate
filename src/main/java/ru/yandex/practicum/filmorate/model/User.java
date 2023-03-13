package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {

    @NotNull(message = "ID должен быть задан.")
    private int id;
    @Email(message = "Email задан некорректно.")
    private String email;
    @NotBlank(message = "Логин не может быть пустым.")
    private String login;
    private String name;
    @PastOrPresent(message = "Дата рождения не может быть в будущем.")
    private LocalDate birthday;
    private Set<Integer> friends = new HashSet<>();

}
