package org.samhsa.c2s.fis.web;

import org.samhsa.c2s.fis.service.ConsentService;
import org.samhsa.c2s.fis.service.dto.PatientConsentDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/consents")
public class ConsentRestController {

    @Autowired
    private ConsentService consentService;

    @PostMapping
    String publishFhirConsent(@Valid @RequestBody PatientConsentDto patientConsentDto) {
        return consentService.publishAndGetAttestedFhirConsent(patientConsentDto.getConsent(),patientConsentDto.getPatient());
    }

    @PutMapping
    String  revokeFhirPConsent(@Valid @RequestBody PatientConsentDto patientConsentDto) {
       return  consentService.revokeAndGetRevokedFhirConsent(patientConsentDto.getConsent(),patientConsentDto.getPatient());
    }

    @GetMapping
    String searchFhirPConsent(@RequestParam String patientMrnSystem, @RequestParam String patientMrn) {
        return  consentService.searchFhirConsents(patientMrnSystem, patientMrn);
    }
}
