package com.example.emr_server.repository;

import com.example.emr_server.entity.DataVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DataVersionRepository extends JpaRepository<DataVersion, UUID> {

    /**
     * Pobiera listę wersji danych dla określonego typu encji.
     *
     * @param entityType typ encji
     * @return lista wersji danych dla podanego typu encji
     */
    List<DataVersion> findByEntityType(String entityType);

    /**
     * Pobiera listę wersji danych powiązanych z podanym identyfikatorem encji.
     *
     * @param entityId UUID encji
     * @return lista wersji danych dla podanego entityId
     */
    List<DataVersion> findByEntityId(UUID entityId);

    /**
     * Pobiera listę wersji danych zmodyfikowanych przez określonego użytkownika.
     *
     * @param modifiedById UUID użytkownika
     * @return lista wersji danych zmodyfikowanych przez podanego użytkownika
     */
    List<DataVersion> findByModifiedBy_Id(UUID modifiedById);

    /**
     * Pobiera listę wersji danych zmodyfikowanych w podanym przedziale czasowym.
     *
     * @param startTime początkowy znacznik czasu
     * @param endTime   końcowy znacznik czasu
     * @return lista wersji danych zmodyfikowanych w określonym okresie
     */
    List<DataVersion> findByModifiedAtBetween(Instant startTime, Instant endTime);

    /**
     * Pobiera listę wersji danych na podstawie typu encji oraz identyfikatora encji.
     *
     * @param entityType typ encji
     * @param entityId   UUID encji
     * @return lista wersji danych powiązanych z podanym typem encji i identyfikatorem encji
     */
    List<DataVersion> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}