--1. create the t_task_order table
drop table if exists t_task_order cascade;
drop sequence if exists seq_t_task_order_id cascade;

create sequence seq_t_task_order_id increment by 1 minvalue 1
  no maxvalue start with 1;

create table t_task_order
(
  id        NUMERIC(18, 0) primary key default nextval('seq_t_task_order_id'),
  name      VARCHAR(64) NOT NULL,
  order_type VARCHAR(32) NOT NULL,
  key       VARCHAR(512) NOT NULL unique ,
  attributes    VARCHAR(1024),
  create_dt timestamp,
  last_update_dt timestamp
);

create index idx_t_task_order_order_key on t_task_order(key);


--2. create the t_task table
drop table if exists t_task cascade;
drop sequence if exists seq_t_task_id cascade;

create sequence seq_t_task_id increment by 1 minvalue 1
  no maxvalue start with 1;

create table t_task
(
  id        NUMERIC(18, 0) primary key default nextval('seq_t_task_id'),
  order_key VARCHAR(128) not null,
  name      VARCHAR(256) not null,
  description VARCHAR(1024),
  execution_key VARCHAR(256) NOT NULL,
  successor_ids VARCHAR(512),
  input   VARCHAR(1024),
  output  VARCHAR(1024),
  async   BOOLEAN default false,
  dummy   BOOLEAN default false,
  create_dt timestamp,
  last_update_dt timestamp,
  status  VARCHAR(64) NOT NULL,
  start_dt   timestamp,
  end_dt  timestamp,
  success   BOOLEAN,
  fail_reason TEXT,
  timeout  integer,
  timeout_unit varchar(32)
);

create index idx_t_task_order_key on t_task(order_key);
