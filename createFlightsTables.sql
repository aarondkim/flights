CREATE TABLE CARRIERS(
    cid varchar(7),
    name varchar(83),
    PRIMARY KEY(cid) 
);

CREATE TABLE MONTHS(
    mid int,
    month varchar(9),
    PRIMARY KEY(mid)
);

CREATE TABLE WEEKDAYS(
    did int,
    day_of_week varchar(9),
    PRIMARY KEY(did)
);

CREATE TABLE FLIGHTS(
    fid int,
    month_id int,
    day_of_month int,
    day_of_week_id int,
    carrier_id varchar(7),
    flight_num int,
    origin_city varchar(34),
    origin_state varchar(47), 
    dest_city varchar(34), 
    dest_state varchar(46), 
    departure_delay int,
    taxi_out int, 
    arrival_delay int,
    canceled int, 
    actual_time int,
    distance int, 
    capacity int,
    price int,
    PRIMARY KEY (fid),
    FOREIGN KEY (carrier_id) REFERENCES CARRIERS (cid),
    FOREIGN KEY (month_id) REFERENCES MONTHS (mid),
    FOREIGN KEY (day_of_week_id) REFERENCES WEEKDAYS (did)  
);
