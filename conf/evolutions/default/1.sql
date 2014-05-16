# Players schema

# --- !Ups

CREATE TABLE player (
    id          integer         NOT NULL,
    firstName   varchar(255)    NOT NULL,
    lastName    varchar(255),
    nickName    varchar(255),
    mail        varchar(255),
    password    varchar(255)    NOT NULL,
    PRIMARY KEY (id)
)

# --- !Downs
DROP TABLE player