package org.samhsa.c2s.fis.service;


import org.hl7.fhir.dstu3.model.Patient;
import org.samhsa.c2s.fis.service.dto.PatientDto;

public interface PatientService {

    /* converts UserDto to fhir patient object */
    public Patient createFhirPatient(PatientDto patientDto);

    public String getPatientResourceId(String patientMrnSystem, String patientMrn);

    public void publishFhirPatient(PatientDto patientDto);

    public void updateFhirPatient(PatientDto patientDto);


}
