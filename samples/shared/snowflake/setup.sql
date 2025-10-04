-- Shared setup script for all samples
-- Creates database, schema, and a sample table with seed data

-- Adjust names as needed
create database if not exists SAMPLES;
use database SAMPLES;
create schema if not exists PUBLIC;
use schema PUBLIC;

-- Create or replace table used by the sample query
CREATE OR REPLACE TABLE SOME_TABLE (
  ID         NUMBER AUTOINCREMENT START 1 INCREMENT 1,
  USER_ID    NUMBER NOT NULL,
  CREATED_AT TIMESTAMP_NTZ NOT NULL,
  AMOUNT     NUMBER(10,2),
  DETAILS    VARIANT,
  PRIMARY KEY (ID)
);

CREATE OR REPLACE PROCEDURE insert_new_sample_row(
    p_user_id NUMBER,
    p_amount  NUMBER(10,2),
    p_details STRING   -- note: STRING here, we’ll parse into VARIANT inside
)
RETURNS STRING
LANGUAGE SQL
AS
$$
BEGIN
    INSERT INTO some_table (user_id, amount, details, created_at)
    SELECT :p_user_id,
           :p_amount,
           PARSE_JSON(:p_details),
           CURRENT_TIMESTAMP;

    RETURN 'Inserted transaction for USER_ID=' || :p_user_id;
END;
$$;

insert into SOME_TABLE (ID, USER_ID, CREATED_AT, AMOUNT, DETAILS)
select 1, 1, to_timestamp_ntz('2025-10-02 10:00:00'), 99.95, parse_json('{"status":"new","type":"order"}') union all
select 2, 1, to_timestamp_ntz('2025-10-10 09:30:00'), 150.00, parse_json('{"status":"shipped","type":"order"}') union all
select 3, 2, to_timestamp_ntz('2025-09-15 12:15:00'), 49.99, parse_json('{"status":"new","type":"order"}') union all
select 4, 3, to_timestamp_ntz('2025-09-20 08:45:00'), 75.50, parse_json('{"status":"cancelled","type":"order"}') union all
select 5, 1, to_timestamp_ntz('2025-10-21 14:05:00'), 10.00, parse_json('{"category":"login","type":"event"}');
