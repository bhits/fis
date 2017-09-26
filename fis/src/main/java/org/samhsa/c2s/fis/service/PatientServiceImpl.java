package org.samhsa.c2s.fis.service;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.IGenericClient;
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
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.samhsa.c2s.fis.config.FisProperties;
import org.samhsa.c2s.fis.service.dto.UserDto;
import org.samhsa.c2s.fis.service.exception.FHIRFormatErrorException;
import org.samhsa.c2s.fis.service.exception.MultiplePatientsFound;
import org.samhsa.c2s.fis.service.exception.PatientNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.function.Function;

@Service
@Slf4j
public class PatientServiceImpl implements PatientService {


    @Autowired
    private FhirContext fhirContext;

    @Autowired
    private IGenericClient fhirClient;

    @Autowired
    private FhirValidator fhirValidator;

    @Autowired
    private FisProperties fisProperties;

    public String getPatientFhirResource(String patientMrn){

        Bundle patientSearchResponse = fhirClient.search()
                .forResource(Patient.class)
                .where(new TokenClientParam("identifier")
                        .exactly()
                        .systemAndCode(fisProperties.getMrn().getCodeSystem(), patientMrn))
                .returnBundle(Bundle.class)
                .execute();


        if(patientSearchResponse == null || patientSearchResponse.getEntry().size() < 1){
            log.debug("No patient found in FHIR server with the given MRN: " + patientMrn);
            throw new PatientNotFound("No patient found for the given MRN");
        }

        if(patientSearchResponse.getEntry().size() > 1){
            log.warn("Multiple patients were found in FHIR server for the same given MRN: " + patientMrn);
            log.debug("       URL of FHIR Server: " + fhirClient.getServerBase());
            throw new MultiplePatientsFound("Multiple patients found in FHIR server with the given MRN");
        }

        Patient patientObj = (Patient) patientSearchResponse.getEntry().get(0).getResource();

        String patientResourceId = patientObj.getIdElement().getIdPart();
        Parameters outParams = fhirClient
                .operation()
                .onInstance(new IdDt("Patient", patientResourceId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .useHttpGet()
                .execute();
        Bundle responseBundle = (Bundle) outParams.getParameter().get(0).getResource();
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(responseBundle);
    }


    @Override
    public void publishFhirPatient(UserDto userDto) {
        final Patient patient = createFhirPatient(userDto);
        if (log.isDebugEnabled()) {
            log.debug("FHIR Patient:");
            log.debug(fhirContext.newXmlParser().setPrettyPrint(true)
                    .encodeResourceToString(patient));
            log.debug(fhirContext.newJsonParser().setPrettyPrint(true)
                    .encodeResourceToString(patient));
        }

        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
            applyRequestEncoding(fhirClient.create().resource(patient)).execute();
        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    @Override
    public void updateFhirPatient(UserDto userDto) {
        final Patient patient = createFhirPatient(userDto);
        final ValidationResult validationResult = fhirValidator.validateWithResult(patient);
        if (validationResult.isSuccessful()) {
            if (fisProperties.getFhir().getPublish().isUseCreateForUpdate()) {
                log.debug("Calling FHIR Patient Create for Update based on the configuration");
                applyRequestEncoding(fhirClient.create().resource(patient)).execute();
            } else {
                log.debug("Calling FHIR Patient Update for Update based on the configuration");
                applyRequestEncoding(fhirClient.update().resource(patient))
                        .conditional()
                        .where(Patient.IDENTIFIER.exactly().systemAndCode(fisProperties.getMrn().getCodeSystem(), patient.getId()))
                        .execute();
            }
        } else {
            throw new FHIRFormatErrorException("FHIR Patient Validation is not successful" + validationResult.getMessages());
        }
    }

    @Override
    public Patient createFhirPatient(UserDto userDto) {
        return userDtoToPatient.apply(userDto);
    }

    private void setIdentifiers(Patient patient, UserDto userDto) {

        //setting patient mrn
        patient.addIdentifier().setSystem(fisProperties.getMrn().getCodeSystem())
                .setUse(Identifier.IdentifierUse.OFFICIAL).setValue(userDto.getMrn());

        patient.setId(new IdType(userDto.getMrn()));

        // setting ssn value
        userDto.getSocialSecurityNumber()
                .map(String::trim)
                .ifPresent(ssnValue -> patient.addIdentifier().setSystem(fisProperties.getSsn().getCodeSystem())
                        .setValue(ssnValue));
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

    Function<UserDto, Patient> userDtoToPatient = new Function<UserDto, Patient>() {
        @Override
        public Patient apply(UserDto userDto) {
            // set patient information
            Patient fhirPatient = new Patient();

            //setting mandatory fields
            fhirPatient.addName().setFamily(userDto.getLastName()).addGiven(userDto.getFirstName());
            fhirPatient.setBirthDate(Date.valueOf(userDto.getBirthDate()));
            fhirPatient.setGender(getPatientGender.apply(userDto.getGenderCode()));
            fhirPatient.setActive(true);

            //Add an Identifier
            setIdentifiers(fhirPatient, userDto);

            //optional fields
            userDto.getAddresses().stream().forEach(addressDto ->
                    fhirPatient.addAddress().addLine(addressDto.getLine1()).addLine(addressDto.getLine2()).setCity(addressDto.getCity()).setState(addressDto.getStateCode()).setPostalCode(addressDto.getPostalCode()).setCountry(addressDto.getCountryCode())
            );

            userDto.getTelecoms().stream().forEach(telecomDto ->
                    fhirPatient.addTelecom().setSystem(ContactPoint.ContactPointSystem.valueOf(telecomDto.getSystem())).setUse(ContactPoint.ContactPointUse.valueOf(telecomDto.getUse())).setValue(telecomDto.getValue())
            );

            return fhirPatient;
        }
    };


}


