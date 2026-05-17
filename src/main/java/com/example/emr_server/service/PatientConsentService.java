package com.example.emr_server.service;

import com.example.emr_server.controller.dto.PatientConsentDto;
import com.example.emr_server.controller.dto.request.ConsentGrantRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PatientConsentService {

    /** Nadaj zgodę (doctor/admin nadaje dostęp innemu userowi do pacjenta). */
    PatientConsentDto grant(ConsentGrantRequest req);

    /** Cofnij zgodę – tylko nadawca lub admin. */
    PatientConsentDto revoke(UUID consentId);

    /** Lista zgód dla danego pacjenta (lekarz/admin z dostępem). */
    Page<PatientConsentDto> listForPatient(UUID patientId, Pageable pageable);

    /** Moje aktywne zgody – jakie dostępy mam do pacjentów innych lekarzy. */
    Page<PatientConsentDto> listMyActiveConsents(Pageable pageable);

    /** Zgody udzielone przeze mnie dla danego pacjenta. */
    Page<PatientConsentDto> listGrantedByMe(UUID patientId, Pageable pageable);
}

