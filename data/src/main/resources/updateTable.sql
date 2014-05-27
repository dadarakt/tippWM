use tippwm

create table lastupdate(
	id VARCHAR(255) NOT NULL,
	lastupdate DATETIME DEFAULT CURRENT_TIMESTAMP
);
