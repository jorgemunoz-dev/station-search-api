package com.petrolprice.station_search_api.infrastructure.out.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petrolprice.station_search_api.domain.model.OpeningPeriod;
import com.petrolprice.station_search_api.domain.model.Station;
import com.petrolprice.station_search_api.domain.type.Country;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.entity.StationOpeningPeriodEntity;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.jpa.JPAStationRepository;
import com.petrolprice.station_search_api.infrastructure.out.persistence.postgres.mapper.StationEntityMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresStationRepositoryTest {

    @Mock
    private JPAStationRepository jpaStationRepository;

    @Mock
    private StationEntityMapper mapper;

    @InjectMocks
    PostgresStationRepository adapter;

    @Test
    void shouldUpdateExistingStationWithoutCallingSave() {
        // Given
        Station stationSnapshot = mock(Station.class);
        StationEntity existingEntity = mock(StationEntity.class);
        Station updatedStation = mock(Station.class);

        when(stationSnapshot.getExternalId()).thenReturn("4375");
        when(stationSnapshot.getCountry()).thenReturn(Country.ES);

        when(existingEntity.getOpeningPeriods()).thenReturn(new ArrayList<>());

        when(mapper.mapOpeningPeriodsToDomain(existingEntity.getOpeningPeriods()))
                .thenReturn(List.of());

        when(stationSnapshot.getOpeningPeriods()).thenReturn(List.of());

        when(jpaStationRepository.findByExternalIdAndCountry("4375", Country.ES))
                .thenReturn(Optional.of(existingEntity));

        when(mapper.toDomain(existingEntity)).thenReturn(updatedStation);

        // When
        Station result = adapter.upsertFromSnapshot(stationSnapshot);

        // Then
        assertThat(result).isSameAs(updatedStation);

        verify(mapper).updateEntityFromDomain(stationSnapshot, existingEntity);
        verify(jpaStationRepository, never()).save(any());
        verify(mapper, never()).mapOpeningPeriods(any());
    }

    @Test
    void shouldReplaceOpeningPeriodsWhenTheyChanged() {
        // Given
        Station stationSnapshot = mock(Station.class);
        StationEntity existingEntity = mock(StationEntity.class);
        Station updatedStation = mock(Station.class);

        List<StationOpeningPeriodEntity> currentOpeningPeriods = new ArrayList<>();
        currentOpeningPeriods.add(mock(StationOpeningPeriodEntity.class));

        List<OpeningPeriod> currentDomainOpeningPeriods = List.of(mock(OpeningPeriod.class));
        List<OpeningPeriod> snapshotOpeningPeriods = List.of(mock(OpeningPeriod.class));

        List<StationOpeningPeriodEntity> newOpeningPeriods =
                List.of(mock(StationOpeningPeriodEntity.class), mock(StationOpeningPeriodEntity.class));

        when(stationSnapshot.getExternalId()).thenReturn("4375");
        when(stationSnapshot.getCountry()).thenReturn(Country.ES);

        when(existingEntity.getOpeningPeriods()).thenReturn(currentOpeningPeriods);

        when(mapper.mapOpeningPeriodsToDomain(currentOpeningPeriods)).thenReturn(currentDomainOpeningPeriods);

        when(stationSnapshot.getOpeningPeriods()).thenReturn(snapshotOpeningPeriods);

        when(jpaStationRepository.findByExternalIdAndCountry("4375", Country.ES))
                .thenReturn(Optional.of(existingEntity));

        when(mapper.mapOpeningPeriods(snapshotOpeningPeriods)).thenReturn(newOpeningPeriods);

        when(mapper.toDomain(existingEntity)).thenReturn(updatedStation);

        // When
        Station result = adapter.upsertFromSnapshot(stationSnapshot);

        // Then
        assertThat(result).isSameAs(updatedStation);
        assertThat(currentOpeningPeriods).containsExactlyElementsOf(newOpeningPeriods);

        verify(mapper).updateEntityFromDomain(stationSnapshot, existingEntity);
        verify(mapper).mapOpeningPeriods(snapshotOpeningPeriods);
        verify(jpaStationRepository, never()).save(any());
    }
}
