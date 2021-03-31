CREATE TABLE IF NOT EXISTS USERS(
    username  VARCHAR(64) NOT NULL PRIMARY KEY,
    password  VARCHAR(64) NOT NULL,
    email     VARCHAR(128),
    firstname VARCHAR(128) NOT NULL,
    lastname  VARCHAR(128) NOT NULL,
    birthdate DATE NOT NULL
);