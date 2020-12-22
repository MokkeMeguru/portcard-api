CREATE TABLE user_topic_images (
    user_topic_uid UUID REFERENCES user_topics(uid),
    user_topic_image_blob VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP default CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
