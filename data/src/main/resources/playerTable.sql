USE tippWM


CREATE TABLE player (
	id integer NOT NULL AUTO_INCREMENT,
	firstname varchar(255) NOT NULL,
	lastname varchar(255) NOT NULL,
	nickname varchar(255),
	email varchar(255) UNIQUE,
	guessfirst varchar(255) NOT NULL,
	guesssecond varchar(255) NOT NULL,
	guessthird varchar(255) NOT NULL,
	tipps1 TEXT,
	scoredMatches TEXT,
	falseMatches TEXT,
	missedMatches TEXT,
	points integer NOT NULL DEFAULT 0,
	pointstime TEXT,
	tendencies integer NOT NULL DEFAULT 0,
	tendenciestime TEXT,
	diffs integer NOT NULL DEFAULT 0,
	diffstime TEXT,
	hits integer NOT NULL DEFAULT 0,
	hitstime TEXT,
	PRIMARY KEY (id)
);
