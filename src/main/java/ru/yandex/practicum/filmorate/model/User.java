package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.LocalDate;

/*
Единственный вопрос, наверное уже на будущие спринты, как я понял, мы их теперь в таком виде будет сдавать, через PullRequest
Лучше для каждой попытки создавать новый запрос или уже работать в созданном?
 */
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

}
