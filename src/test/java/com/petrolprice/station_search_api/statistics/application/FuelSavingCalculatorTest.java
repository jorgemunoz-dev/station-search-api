package com.petrolprice.station_search_api.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FuelSavingCalculatorTest {
    private final FuelSavingCalculator calculator = new FuelSavingCalculator();

    @Test
    void shouldUseAFiftyFiveLiterTankByDefault() {
        var saving = calculator.calculate(new BigDecimal("1.400"), new BigDecimal("1.600"), null);

        assertThat(saving.savingPerLiter()).isEqualByComparingTo("0.200");
        assertThat(saving.tankLiters()).isEqualByComparingTo("55");
        assertThat(saving.savingPerTank()).isEqualByComparingTo("11.000");
    }
}
