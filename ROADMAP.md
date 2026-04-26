## station-search-api Roadmap

### MVP

- [ ] STA-01 - Store stations with current prices
  - [ ] Listen rabbitmq events with station snapshots
  - [ ] Add geospatial index

- [ ] STA-02 - Store historical prices
  - [ ] Move current prices to historical prices
  - [ ] Add geospatial index

- [ ] STA-03 - API Search stations by radius
  - [ ] Filter by fuel type
  - [ ] Sort by price
  - [ ] Sort by distance
  - [ ] Filter closest station
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