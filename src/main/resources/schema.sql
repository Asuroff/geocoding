CREATE TABLE request_log (
    id IDENTITY PRIMARY KEY,
    lat DOUBLE NOT NULL,
    lon DOUBLE NOT NULL,
    request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE response_log (
    id IDENTITY PRIMARY KEY,
    request_id BIGINT,
    response CLOB,
    response_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES request_log(id)
);
