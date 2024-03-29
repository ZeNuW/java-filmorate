package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
public class User {

    @NotNull(message = "ID должен быть задан.")
    private int id;
    private String name;
    @NotBlank(message = "Логин не может быть пустым.")
    private String login;
    @Email(message = "Email задан некорректно.")
    private String email;
    @PastOrPresent(message = "Дата рождения не может быть в будущем.")
    private LocalDate birthday;
    @JsonIgnore
    private transient Set<Integer> friends;
}
