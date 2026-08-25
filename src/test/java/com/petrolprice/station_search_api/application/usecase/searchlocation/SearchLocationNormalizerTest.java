package com.petrolprice.station_search_api.application.usecase.searchlocation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchLocationNormalizerTest {

    @InjectMocks
    SearchLocationNormalizer normalizer;

    @Test
    void shouldNormalizeLocalityName() {
        String result = normalizer.normalizeText("  Málaga  ");

        assertThat(result).isEqualTo("malaga");
    }

    @Test
    void shouldRemovePunctuationAndCollapseSpaces() {
        String result = normalizer.normalizeText("L'Hospitalet  de--Llobregat");

        assertThat(result).isEqualTo("l hospitalet de llobregat");
    }

    @Test
    void shouldNormalizeInternationalCharacters() {
        String result = normalizer.normalizeText("Île-de-France");

        assertThat(result).isEqualTo("ile de france");
    }

    @Test
    void shouldReturnEmptyStringWhenValueIsNull() {
        assertThat(normalizer.normalizeText(null)).isEmpty();
    }

    @Test
    void shouldNormalizePostalCode() {
        String result = normalizer.normalizePostalCode(" sw1a 1aa ");

        assertThat(result).isEqualTo("SW1A1AA");
    }

    @Test
    void shouldRemovePostalCodeSeparators() {
        String result = normalizer.normalizePostalCode("1000-001");

        assertThat(result).isEqualTo("1000001");
    }
}
