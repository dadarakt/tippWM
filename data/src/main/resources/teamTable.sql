USE tippwm

CREATE TABLE team (
	id integer NOT NULL AUTO_INCREMENT,
	name varchar(255) NOT NULL UNIQUE,
	groupchar char NOT NULL,
	iconurl varchar(255) NOT NULL DEFAULT 'http://img3.wikia.nocookie.net/__cb20080619042358/football/en/images/5/5c/Football.png',
	gamesplayed integer NOT NULL DEFAULT 0,
	wins integer NOT NULL DEFAULT 0,
	losses integer NOT NULL DEFAULT 0,
	draws integer NOT NULL DEFAULT 0,
	goalsscored integer NOT NULL DEFAULT 0,
	goalsgotten integer NOT NULL DEFAULT 0,
	points integer NOT NULL DEFAULT 0,
	PRIMARY KEY (id)
);
