
 CREATE TABLE recommendation_default (
   id_user BIGINT NOT NULL,
   id_item BIGINT NOT NULL,
   preference_value FLOAT NOT NULL,
   PRIMARY KEY (id_user, id_item),
   INDEX (id_user),
   INDEX (id_item)
 );
 