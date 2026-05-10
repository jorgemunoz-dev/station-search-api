package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper;

import com.petrolprice.station_search_api.domain.model.GeoLocation;
import java.math.BigDecimal;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class GeoLocationEntityMapper {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public Point toPoint(GeoLocation location) {
        if (location == null) {
            return null;
        }

        Point point = geometryFactory.createPoint(new Coordinate(
                location.getLongitude().doubleValue(), location.getLatitude().doubleValue()));

        point.setSRID(4326);
        return point;
    }

    public GeoLocation toDomain(Point point) {
        if (point == null) {
            return null;
        }

        return GeoLocation.builder()
                .latitude(BigDecimal.valueOf(point.getY()))
                .longitude(BigDecimal.valueOf(point.getX()))
                .build();
    }
}
