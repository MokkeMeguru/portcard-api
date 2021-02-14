CREATE TABLE contacts (
       uid UUID PRIMARY KEY,
       user_uid VARCHAR(255) REFERENCES users(uid),
       subject VARCHAR(255),
       contact_from VARCHAR(512),
       created_at TIMESTAMP default CURRENT_TIMESTAMP
);
