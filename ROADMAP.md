## station-search-api Roadmap

### MVP

- [X] STA-01 - Store stations with current prices
  - [X] Listen rabbitmq events with station snapshots
  - [X] Add geospatial index

- [X] STA-02 - Store historical prices
  - [X] Move current prices to historical prices

- [X] STA-03 - API Search stations by radius
  - [X] Filter by coordinates
  - [X] Filter by fuel type
  - [X] Sort by price
  - [X] Sort by distance
  - [X] Filter closest station
- [ ] STA-04 - API Search stations by locality
  - [ ] Reuse filters
  - [ ] Create search_area table with country / locality name / zip / coordinates
- [ ] STA-05 - API Search stations by coordinates
- [ ] STA-06 - Implement best option scoring
- [ ] STA-07 - Calculate estimated savings

### Post-MVP

- [ ] Price history endpoint
- [ ] Price trend analysis
- [ ] Advanced EV filters
- [ ] Route-based station suggestions
- [ ] Cache frequent geospatial searches
