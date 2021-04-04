CREATE TABLE IF NOT EXISTS users (
    id             BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    username       VARCHAR(64) NOT NULL,
    password       VARCHAR(64),
    data           JSONB NOT NULL DEFAULT '{}'::JSONB
);

CREATE INDEX idx_users_username ON users(username);

INSERT INTO users (username, password, data) VALUES
    ('carmen', 'carmen_p', '{"email":"carmen@tra.vis", "firstName":"Carmen", "lastName":"McCallum"}'::jsonb),
    ('vlad', 'vlap_p', '{"email":"vlad@tra.vis", "firstName":"Vlad", "lastName":"Nyrki"}'::jsonb),
    ('travis', 'travis_p', '{"email":"travis@tra.vis", "firstName":"Steve", "lastName":"Travis"}'::jsonb);