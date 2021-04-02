CREATE TABLE IF NOT EXISTS users (
    id        INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username  VARCHAR(64) NOT NULL,
    password  VARCHAR(64) NOT NULL,
    email     VARCHAR(128),
    firstname VARCHAR(128) NOT NULL,
    lastname  VARCHAR(128) NOT NULL
);

CREATE INDEX idx_users_username ON users(username);

INSERT INTO users (username, password, email, firstname, lastname) VALUES
    ('carmen', 'carmen_p', 'carmen@tra.vis', 'Carmen', 'McCallum'),
    ('vlad', 'vlap_p', 'vlad@tra.vis', 'Vlad', 'Nyrki'),
    ('travis', 'travis_p', 'travis@tra.vis', 'Steve', 'Travis');