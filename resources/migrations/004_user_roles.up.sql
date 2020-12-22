CREATE TABLE user_roles (
       uid UUID PRIMARY KEY,
       user_uid VARCHAR(255) REFERENCES users(uid),
       category SMALLINT NOT NULL,
       primary_rank SMALLINT NOT NULL,
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP,
       UNIQUE (user_uid, primary_rank),
       UNIQUE (user_uid, category)
);
