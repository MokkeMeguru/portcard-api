ALTER TABLE user_role_links DROP COLUMN link_category;
ALTER TABLE user_role_links ADD COLUMN link_category_name varchar(64) NOT NULL DEFAULT 'others';
