DROP TABLE user_topics CASCADE;
CREATE TABLE user_topics (
       uid UUID UNIQUE,
       idx SERIAL PRIMARY KEY,
       user_uid VARCHAR(255) REFERENCES users(uid),
       title VARCHAR(255) NOT NULL,
       category SMALLINT NOT NULL,
       description text,
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP
);
