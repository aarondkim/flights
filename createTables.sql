CREATE TABLE Users(username VARCHAR(20) PRIMARY KEY,
                   hashedPassword VARBINARY(256) NOT NULL,
                   balance INT NOT NULL);

CREATE TABLE Reservations(res_id INT PRIMARY KEY,
                          paid INT NOT NULL,
                          username VARCHAR(20) NOT NULL REFERENCES Users(username),
                          fid1 INT NOT NULL REFERENCES FLIGHTS(fid),
                          fid2 INT REFERENCES FLIGHTS(fid));