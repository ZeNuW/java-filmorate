CREATE TABLE IF NOT EXISTS genres
(
    genre_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(100) UNIQUE
);

CREATE TABLE IF NOT EXISTS mpa
(
    mpa_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name   VARCHAR(100) UNIQUE
);

INSERT INTO genres (name)
SELECT name
FROM (
         VALUES ('Комедия'),
                ('Драма'),
                ('Мультфильм'),
                ('Триллер'),
                ('Документальный'),
                ('Боевик')
         ) AS t(name)
WHERE NOT EXISTS(SELECT * FROM genres);

INSERT INTO mpa(name)
SELECT name
FROM (
         VALUES ('G'),
                ('PG'),
                ('PG-13'),
                ('R'),
                ('NC-17')
         ) AS t(name)
WHERE NOT EXISTS(SELECT * FROM mpa);

CREATE TABLE IF NOT EXISTS users
(
    user_id  INTEGER     NOT NULL PRIMARY KEY,
    name     VARCHAR(64),
    login    VARCHAR(20) NOT NULL,
    email    VARCHAR(20) NOT NULL,
    birthday DATE
);

CREATE TABLE IF NOT EXISTS friends
(
    user_id          INTEGER NOT NULL,
    friend_id        INTEGER NOT NULL,
    confirmed_status BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    FOREIGN KEY (friend_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS film
(
    film_id      INTEGER NOT NULL PRIMARY KEY,
    name         VARCHAR(64),
    description  VARCHAR(200),
    release_date DATE    NOT NULL,
    duration     INTEGER,
    mpa_id       INTEGER REFERENCES mpa (mpa_id)
);

CREATE TABLE IF NOT EXISTS film_genres
(
    film_id  INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES film (film_id),
    FOREIGN KEY (genre_id) REFERENCES genres (genre_id)
);


CREATE TABLE IF NOT EXISTS film_likes
(
    film_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES film (film_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);
