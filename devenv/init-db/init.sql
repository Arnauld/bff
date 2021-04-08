CREATE TABLE IF NOT EXISTS users (
    id             BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    username       VARCHAR(64) NOT NULL,
    password       VARCHAR(255),
    data           JSONB NOT NULL DEFAULT '{}'::JSONB
);

CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE UNIQUE INDEX idx_users_email    ON users((lower(data->>'email')));

CREATE extension pgcrypto;

INSERT INTO users (username, password, data) VALUES
    ('carmen', crypt('carmen_p', gen_salt('bf', 10)), '{"email":"carmen@tra.vis", "firstName":"Carmen", "lastName":"McCallum"}'::jsonb),
    ('vlad',   crypt('vlap_p',   gen_salt('bf', 10)), '{                          "firstName":"Vlad",   "lastName":"Nyrki"   }'::jsonb),
    ('travis', crypt('travis_p', gen_salt('bf', 10)), '{"email":"travis@tra.vis", "firstName":"Steve",  "lastName":"Travis"  }'::jsonb),
    ('Miss Thundercat', null, '{}'::jsonb);

-- Insert huge amount of users...
-- all with the same password due to blowfish (timeconsumiiing) purpose
INSERT INTO users(username, password) 
    SELECT 
        to_char(g, 'fm000000'),
--        crypt(to_char(g, 'fm000000'), gen_salt('bf', 4)) 
        p
    FROM generate_series(1, 100000) as g,
         crypt('admin', gen_salt('bf', 10)) as p;
