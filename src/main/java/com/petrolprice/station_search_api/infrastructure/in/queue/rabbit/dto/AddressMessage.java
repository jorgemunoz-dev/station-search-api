package com.petrolprice.station_search_api.infrastructure.in.queue.rabbit.dto;

public record AddressMessage(String street, String postalCode, String locality, String municipality, String province) {}
