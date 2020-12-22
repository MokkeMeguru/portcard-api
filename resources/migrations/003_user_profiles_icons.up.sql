CREATE TABLE user_profiles_icons (
       user_uid VARCHAR(255) REFERENCES users(uid) UNIQUE,
       icon_blob VARCHAR(255) NOT NULL UNIQUE,
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP
);
