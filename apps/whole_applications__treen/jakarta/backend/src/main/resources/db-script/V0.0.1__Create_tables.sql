CREATE TABLE user_data
( id BIGINT NOT NULL AUTO_INCREMENT
, login VARCHAR(120) NOT NULL
, password_hash VARBINARY(128) NOT NULL
, password_salt VARBINARY(128) NOT NULL
, notebook_version BIGINT NOT NULL DEFAULT 1
, CONSTRAINT user_data_pk PRIMARY KEY (id)
, CONSTRAINT user_data_login_uq UNIQUE (login)
);

CREATE TABLE note
( id BIGINT NOT NULL AUTO_INCREMENT
, parent_id BIGINT
, user_id BIGINT NOT NULL
, title VARCHAR(150)
, link VARCHAR(256)
, description VARCHAR(2000)
, CONSTRAINT note_pk PRIMARY KEY (id)
, CONSTRAINT note_parent_id_fk FOREIGN KEY (parent_id) REFERENCES note (id) ON DELETE CASCADE
, CONSTRAINT note_user_id_fk FOREIGN KEY (user_id) REFERENCES user_data (id)
);
