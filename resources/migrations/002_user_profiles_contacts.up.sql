CREATE TABLE user_profiles_contacts (
    user_uid VARCHAR(255) REFERENCES users(uid) UNIQUE,
    email VARCHAR(511),
    twitter VARCHAR(255),
    facebook VARCHAR(255),
    created_at TIMESTAMP default CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
