package org.samhsa.c2s.fis.service;


import org.samhsa.c2s.fis.service.dto.ConsentDto;
import org.samhsa.c2s.fis.service.dto.PatientDto;


public interface ConsentService {
    String publishAndGetAttestedFhirConsent(ConsentDto consent, PatientDto patient);

    String revokeAndGetRevokedFhirConsent(ConsentDto consent, PatientDto patient);
}
