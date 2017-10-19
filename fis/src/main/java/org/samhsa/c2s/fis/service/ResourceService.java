package org.samhsa.c2s.fis.service;


public interface ResourceService {

    public String getFhirResourceByPatientIdentifier(String patientIdentifierSystem, String patientIdentifierValue) ;

}
