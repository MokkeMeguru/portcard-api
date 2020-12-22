CREATE TABLE users (
    uid VARCHAR(255) PRIMARY KEY,
    uname VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP default CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL default FALSE
);
