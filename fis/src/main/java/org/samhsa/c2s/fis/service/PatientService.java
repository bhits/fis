package org.samhsa.c2s.fis.service;


import org.hl7.fhir.dstu3.model.Patient;
import org.samhsa.c2s.fis.service.dto.UserDto;

public interface PatientService {

    /* converts UserDto to fhir patient object */
    public Patient createFhirPatient(UserDto userDto);

    public void publishFhirPatient(UserDto userDto);

    public void updateFhirPatient(UserDto userDto);


}
