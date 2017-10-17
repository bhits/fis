package org.samhsa.c2s.fis.service;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IClientExecutable;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.samhsa.c2s.fis.config.FisProperties;
import org.samhsa.c2s.fis.service.dto.IdentifierDto;
import org.samhsa.c2s.fis.service.dto.PatientDto;
import org.samhsa.c2s.fis.service.exception.FHIRFormatErrorException;
import org.samhsa.c2s.fis.service.exception.MultiplePatientsFoundException;
import org.samhsa.c2s.fis.service.exception.PatientNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {


    @Autowired
    private FhirContext fhirContext;

    private Map<Class<? extends Resource>, IGenericClient> fhirClients;

    private IParser fhirJsonParser;

    private FhirValidator fhirValidator;

    private FisProperties fisProperties;

    private IGenericClient patientFhirClient;

    @Autowired
    public PatientServiceImpl(FhirContext fhirContext, FhirValidator fhirValidator, Map<Class<? extends Resource>, IGenericClient> fhirClients, FisProperties fisProperties) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirValidator;
        this.fhirClients = fhirClients;
        this.fisProperties=fisProperties;
        this.patientFhirClient = fhirClients.getOrDefault(Patient.class, fhirClients.get(Resource.class));

    }

    @Override
    public void publishFhirPatient(PatientDto patientDto) {
        final Patient patient = createFhirPatient(patientDto);
        if (log.isDebugEnabled()) {
            log.debug("FHIR Patient:");
            log.debug(fhirContext.newXmlParser().setPrettyPrint(true)
                    .encodeResourceToString(patient));
            log.debug(fhirContext.newJsonParser().setPrettyPrint(true)
                    .encodeResourceToString(patient));
        }

        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
            applyRequestEncoding(patientFhirClient.create().resource(patient)).execute();
        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    @Override
    public void updateFhirPatient(PatientDto patientDto) {
        final Patient patient = createFhirPatient(patientDto);
        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
            if (fisProperties.getFhir().getPublish().isUseCreateForUpdate()) {
                log.debug("Calling FHIR Patient Create for Update based on the configuration");
                applyRequestEncoding(patientFhirClient.create().resource(patient)).execute();
            } else {
                log.debug("Calling FHIR Patient Update for Update based on the configuration");
                applyRequestEncoding(patientFhirClient.update().resource(patient))
                        .conditional()
                        .where(Patient.IDENTIFIER.exactly().systemAndCode(getCodeSystemByValue(patientDto.getIdentifiers().get(),patient.getId()), patient.getId()))
                        .execute();
            }
        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    private String getCodeSystemByValue(List<IdentifierDto> identifierList, String value){
        return  identifierList.stream().filter(identifier -> identifier.getValue().equalsIgnoreCase(value)).findFirst().get().getSystem();
    }

    @Override
    public Patient createFhirPatient(PatientDto patientDto) {
        return patientDtoToPatient.apply(patientDto);
    }

    private void setIdentifiers(Patient patient, PatientDto patientDto) {

        //setting patient mrn
        patient.addIdentifier().setSystem(getCodeSystemByValue(patientDto.getIdentifiers().get(),patientDto.getMrn()))
                .setUse(Identifier.IdentifierUse.OFFICIAL).setValue(patientDto.getMrn());

        patient.setId(new IdType(patientDto.getMrn()));

        // setting ssn value
        patientDto.getSocialSecurityNumber()
                .map(String::trim)
                .ifPresent(ssnValue -> patient.addIdentifier().setSystem(getCodeSystemByValue(patientDto.getIdentifiers().get(),patientDto.getSocialSecurityNumber().get())).setValue(ssnValue));
    }

    private ICreateTyped applyRequestEncoding(ICreateTyped request) {
        return (ICreateTyped) applyRequestEncodingFromConfig(request);
    }

    private IUpdateTyped applyRequestEncoding(IUpdateTyped request) {
        return (IUpdateTyped) applyRequestEncodingFromConfig(request);
    }

    private IClientExecutable applyRequestEncodingFromConfig(IClientExecutable request) {
        switch (fisProperties.getFhir().getPublish().getEncoding()) {
            case XML:
                request.encodedXml();
                break;
            case JSON:
            default:
                request.encodedJson();
                break;
        }
        return request;
    }

     Function<String, Enumerations.AdministrativeGender> getPatientGender = new Function<String, Enumerations.AdministrativeGender>() {
        @Override
        public Enumerations.AdministrativeGender apply(String codeString) {
            switch (codeString.toUpperCase()) {
                case "MALE":
                    return Enumerations.AdministrativeGender.MALE;
                case "M":
                    return Enumerations.AdministrativeGender.MALE;
                case "FEMALE":
                    return Enumerations.AdministrativeGender.FEMALE;
                case "F":
                    return Enumerations.AdministrativeGender.FEMALE;
                case "OTHER":
                    return Enumerations.AdministrativeGender.OTHER;
                case "O":
                    return Enumerations.AdministrativeGender.OTHER;
                case "UNKNOWN":
                    return Enumerations.AdministrativeGender.UNKNOWN;
                case "UN":
                    return Enumerations.AdministrativeGender.UNKNOWN;
                default:
                    return Enumerations.AdministrativeGender.UNKNOWN;

            }

        }
    };

    Function<PatientDto, Patient> patientDtoToPatient = new Function<PatientDto, Patient>() {
        @Override
        public Patient apply(PatientDto patientDto) {
            // set patient information
            Patient fhirPatient = new Patient();

            //setting mandatory fields
            fhirPatient.addName().setFamily(patientDto.getLastName()).addGiven(patientDto.getFirstName());
            fhirPatient.setBirthDate(Date.valueOf(patientDto.getBirthDate()));
            fhirPatient.setGender(getPatientGender.apply(patientDto.getGenderCode()));
            fhirPatient.setActive(true);

            //Add an Identifier
            setIdentifiers(fhirPatient, patientDto);

            //optional fields
            patientDto.getAddresses().stream().forEach(addressDto ->
                    fhirPatient.addAddress().addLine(addressDto.getLine1()).addLine(addressDto.getLine2()).setCity(addressDto.getCity()).setState(addressDto.getStateCode()).setPostalCode(addressDto.getPostalCode()).setCountry(addressDto.getCountryCode())
            );

            patientDto.getTelecoms().stream().forEach(telecomDto ->
                    fhirPatient.addTelecom().setSystem(ContactPoint.ContactPointSystem.valueOf(telecomDto.getSystem())).setUse(ContactPoint.ContactPointUse.valueOf(telecomDto.getUse())).setValue(telecomDto.getValue())
            );

            return fhirPatient;
        }
    };


    @Override
    public String  getPatientResourceId(String patientMrnSystem,  String patientMrn){

        Bundle patientSearchResponse = patientFhirClient.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(patientMrnSystem, patientMrn))
                .returnBundle(Bundle.class)
                .execute();

        if(patientSearchResponse == null || patientSearchResponse.getEntry().size() < 1){
            throw new PatientNotFoundException("No patient found for the given MRN:" + patientMrn + " in FHIR Server" + patientFhirClient.getServerBase());
        }

        if(patientSearchResponse.getEntry().size() > 1){
            throw new MultiplePatientsFoundException("Multiple patients found for the given MRN:" + patientMrn + " in FHIR Server" + patientFhirClient.getServerBase());
        }

        Patient patientObj = (Patient) patientSearchResponse.getEntry().get(0).getResource();

        return patientObj.getIdElement().getIdPart();
    }

}


