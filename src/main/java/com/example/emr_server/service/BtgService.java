package com.example.emr_server.service;

import com.example.emr_server.entity.PatientConsent;

import java.util.Optional;
import java.util.UUID;

public interface BtgService {
    PatientConsent grantBtgConsent(UUID patientId, int minutes, String reason);
    Optional<PatientConsent> getActiveBtgConsent(UUID patientId);
    boolean revokeBtgConsent(UUID patientId);
}
