package com.example.emr_server.service;

import com.example.emr_server.entity.DataVersion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DataVersionService {

    /**
     * Pobiera listę wersji danych dla określonego typu encji.
     *
     * @param entityType typ encji
     * @return lista wersji danych dla podanego typu encji
     */
    List<DataVersion> getDataVersionsByEntityType(String entityType);

    /**
     * Pobiera listę wersji danych powiązanych z podanym identyfikatorem encji.
     *
     * @param entityId UUID encji
     * @return lista wersji danych dla podanego entityId
     */
    List<DataVersion> getDataVersionsByEntityId(UUID entityId);

    /**
     * Pobiera listę wersji danych zmodyfikowanych przez określonego użytkownika.
     *
     * @param modifiedById UUID użytkownika
     * @return lista wersji danych zmodyfikowanych przez podanego użytkownika
     */
    List<DataVersion> getDataVersionsByModifiedBy(UUID modifiedById);

    /**
     * Pobiera listę wersji danych zmodyfikowanych w podanym przedziale czasowym.
     *
     * @param startTime początkowy znacznik czasu
     * @param endTime   końcowy znacznik czasu
     * @return lista wersji danych zmodyfikowanych w określonym okresie
     */
    List<DataVersion> getDataVersionsByModifiedAtRange(Instant startTime, Instant endTime);

    /**
     * Pobiera listę wersji danych na podstawie typu encji oraz identyfikatora encji.
     *
     * @param entityType typ encji
     * @param entityId   UUID encji
     * @return lista wersji danych powiązanych z podanym typem encji i identyfikatorem encji
     */
    List<DataVersion> getDataVersionsByEntityTypeAndEntityId(String entityType, UUID entityId);

    /**
     * Zapisuje nową wersję danych.
     *
     * @param dataVersion obiekt wersji danych do zapisania
     * @return zapisany obiekt wersji danych
     */
    DataVersion saveDataVersion(DataVersion dataVersion);

    /**
     * Usuwa wersję danych na podstawie jej identyfikatora.
     *
     * @param dataVersionId UUID wersji danych do usunięcia
     */
    void deleteDataVersionById(UUID dataVersionId);
}