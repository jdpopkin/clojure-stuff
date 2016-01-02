begin;

create table league_snapshot (
       id serial primary key,
       region text,
       created timestamp not null default now(),
       ranks jsonb not null
);

commit;
