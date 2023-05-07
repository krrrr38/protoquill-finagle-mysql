CREATE TABLE book
(
    id          VARCHAR(255) NOT NULL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    owned_at    DATETIME(6) NOT NULL DEFAULT NOW(6),
    created_at  DATETIME(6) NOT NULL DEFAULT NOW(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT NOW(6) ON UPDATE CURRENT_TIMESTAMP (6)
);

CREATE TABLE category
(
    id         VARCHAR(255) NOT NULL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT NOW(6)
);

CREATE TABLE book_category
(
    book_id     VARCHAR(255) NOT NULL,
    category_id VARCHAR(255) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT NOW(6),
    FOREIGN KEY fk1(book_id) REFERENCES book(id),
    FOREIGN KEY fk2(category_id) REFERENCES category(id)
);

CREATE TABLE encoder_decoder
(
    id                        VARCHAR(255)   NOT NULL PRIMARY KEY,
    string_varchar            VARCHAR(255)   NOT NULL,
    big_decimal_decimal       DECIMAL(36, 9) NOT NULL,
    boolean_tinyint           TINYINT        NOT NULL,
    boolean_bigint            BIGINT         NOT NULL,
    boolean_bit               BIT            NOT NULL,
    int_int                   INT            NOT NULL,
    long_bigint               BIGINT         NOT NULL,
    float_float               FLOAT          NOT NULL,
    double_double             DOUBLE         NOT NULL,
    date_timestamp            TIMESTAMP      NOT NULL,
    local_date_timestamp      TIMESTAMP      NOT NULL,
    local_date_date           DATE           NOT NULL,
    local_date_time_timestamp TIMESTAMP      NOT NULL,
    local_date_time_datetime DATETIME(6)      NOT NULL,
    uuid_varchar              VARCHAR(255)   NOT NULL
)