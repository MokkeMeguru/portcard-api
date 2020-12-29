ALTER TABLE user_role_links ADD COLUMN link_category SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE user_role_links DROP COLUMN link_category_name;
