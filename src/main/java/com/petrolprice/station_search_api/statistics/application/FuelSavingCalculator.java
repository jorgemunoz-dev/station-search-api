package com.petrolprice.station_search_api.statistics.application;

import com.petrolprice.station_search_api.statistics.application.result.FuelSaving;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FuelSavingCalculator {
    public static final BigDecimal DEFAULT_TANK_LITERS = BigDecimal.valueOf(55);

    public FuelSaving calculate(BigDecimal stationPrice, BigDecimal referencePrice, BigDecimal tankLiters) {
        if (stationPrice == null
                || referencePrice == null
                || stationPrice.signum() < 0
                || referencePrice.signum() < 0) {
            throw new IllegalArgumentException("Valid station and reference prices are required");
        }
        BigDecimal effectiveTank = tankLiters == null ? DEFAULT_TANK_LITERS : tankLiters;
        if (effectiveTank.signum() <= 0) {
            throw new IllegalArgumentException("tankLiters must be greater than zero");
        }
        BigDecimal perLiter = referencePrice.subtract(stationPrice);
        return new FuelSaving(stationPrice, referencePrice, perLiter, effectiveTank, perLiter.multiply(effectiveTank));
    }
}
