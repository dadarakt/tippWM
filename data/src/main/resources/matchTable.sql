USE tippwm

CREATE TABLE matches (
	id integer NOT NULL AUTO_INCREMENT,
	teama varchar(255) NOT NULL,
	teamb varchar(255) NOT NULL,
	groupchar char NOT NULL,
	date DATETIME NOT NULL,
	location varchar(255) NOT NULL,
	stadium varchar(255),
	onlineid int NOT NULL UNIQUE,
	groupid integer NOT NULL,
	grouporderid integer NOT NULL,
	groupname varchar(255),
	isfinished BOOLEAN NOT NULL DEFAULT FALSE,
	scorea integer,
	scoreb integer,
	PRIMARY KEY (id)
);
