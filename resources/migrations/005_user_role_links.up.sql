CREATE TABLE user_role_links (
       uid UUID PRIMARY KEY,
       user_role_uid UUID REFERENCES user_roles(uid) ON DELETE CASCADE,
       link_category SMALLINT NOT NULL,
       link_blob VARCHAR(511) NOT NULL,
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP
);
