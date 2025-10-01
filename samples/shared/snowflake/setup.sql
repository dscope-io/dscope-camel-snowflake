-- Shared setup script for all samples
-- Creates database, schema, and a sample table with seed data

-- Adjust names as needed
create database if not exists YOUR_DB;
use database YOUR_DB;
create schema if not exists PUBLIC;
use schema PUBLIC;

create or replace table SOME_TABLE (
  ID         number,
  USER_ID    number,
  CREATED_AT timestamp,
  AMOUNT     number(10,2),
  DETAILS    variant
);

insert into SOME_TABLE (ID, USER_ID, CREATED_AT, AMOUNT, DETAILS)
select 1, 1, to_timestamp_ntz('2025-10-02 10:00:00'), 99.95, parse_json('{"status":"new","type":"order"}') union all
select 2, 1, to_timestamp_ntz('2025-10-10 09:30:00'), 150.00, parse_json('{"status":"shipped","type":"order"}') union all
select 3, 2, to_timestamp_ntz('2025-09-15 12:15:00'), 49.99, parse_json('{"status":"new","type":"order"}') union all
select 4, 3, to_timestamp_ntz('2025-09-20 08:45:00'), 75.50, parse_json('{"status":"cancelled","type":"order"}') union all
select 5, 1, to_timestamp_ntz('2025-10-21 14:05:00'), 10.00, parse_json('{"category":"login","type":"event"}');
