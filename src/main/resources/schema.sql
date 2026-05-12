DROP TABLE IF EXISTS "user" CASCADE;

CREATE TABLE "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255)
);