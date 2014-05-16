# Players schema

# --- !Ups

CREATE TABLE player (
    id          integer         NOT NULL,
    firstName   varchar(255),
    lastName    varchar(255),
    nickName    varchar(255),
    password    varchar(255)
)

# --- !Downs
DROP TABLE player