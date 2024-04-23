create index Flights_idx1 on Flights(origin_city,dest_city,actual_time);
create index Flights_idx2 on Flights(actual_time);
create index Flights_idx3 on Flights(dest_city,origin_city,actual_time);