package org.samhsa.c2s.fis.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Composition;
import org.hl7.fhir.dstu3.model.Consent;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.r4.model.codesystems.V3ActCode;
import org.hl7.fhir.r4.model.codesystems.V3ParticipationType;
import org.samhsa.c2s.fis.config.FisProperties;
import org.samhsa.c2s.fis.infrastructure.VssClient;
import org.samhsa.c2s.fis.service.dto.ConsentDto;
import org.samhsa.c2s.fis.service.dto.OrganizationDto;
import org.samhsa.c2s.fis.service.dto.PatientDto;
import org.samhsa.c2s.fis.service.dto.PractitionerDto;
import org.samhsa.c2s.fis.service.dto.PurposeDto;
import org.samhsa.c2s.fis.service.dto.ValueSetCategoryDto;
import org.samhsa.c2s.fis.service.exception.FHIRFormatErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConsentServiceImpl implements ConsentService {


    private final FhirContext fhirContext;

    private final FhirValidator fhirValidator;

    private Map<Class<? extends Resource>, IGenericClient> fhirClients;

    private final PatientService patientService;

    private final FisProperties fisProperties;

    private final IGenericClient consentFhirClient;

    private final VssClient vssClient;

    private IParser fhirJsonParser;

    @Autowired
    public ConsentServiceImpl(FhirContext fhirContext, FhirValidator fhirValidator, Map<Class<? extends Resource>, IGenericClient> fhirClients, PatientService patientService, VssClient vssClient, FisProperties fisProperties, IParser fhirJsonParser) {
        this.fhirContext = fhirContext;
        this.fhirValidator = fhirValidator;
        this.fhirClients = fhirClients;
        this.patientService = patientService;
        this.vssClient = vssClient;
        this.fisProperties=fisProperties;
        this.fhirJsonParser=fhirJsonParser;
        this.consentFhirClient = fhirClients.getOrDefault(Consent.class, fhirClients.get(Resource.class));

    }


    @Override
    public String publishAndGetAttestedFhirConsent(ConsentDto c2sConsent, PatientDto patientDto) {
        Consent fhirConsent = createFhirConsent(c2sConsent, patientDto);

        // Validate the resource
        ValidationResult validationResult = fhirValidator.validateWithResult(fhirConsent);

        log.debug("validationResult.isSuccessful(): " + validationResult.isSuccessful());
        // Throw format error if the validation is not successful
        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Consent Validation is not successful" + validationResult.getMessages());
        }
        // Publish FHIR consent to FHIR server
        consentFhirClient.create().resource(fhirConsent).execute();

        return fhirContext.newJsonParser().setPrettyPrint(true)
                .encodeResourceToString(fhirConsent);

    }

    @Override
    public String revokeAndGetRevokedFhirConsent(ConsentDto c2sConsent, PatientDto patientDto) {

        // Consent by identifier on FHIR server
        //TODO: Get consent from server and then update the status instead
        Consent fhirConsent = createFhirConsent(c2sConsent, patientDto);
        fhirConsent.setStatus(Consent.ConsentState.INACTIVE);

        // Validate the resource
        ValidationResult validationResult = fhirValidator.validateWithResult(fhirConsent);

        log.debug("validationResult.isSuccessful(): " + validationResult.isSuccessful());
        // Throw format error if the validation is not successful
        if (!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("Consent Validation is not successful" + validationResult.getMessages());
        }

        // Revoke FHIR consent to FHIR server
        consentFhirClient.update().resource(fhirConsent)
                .conditional()
                .where(Consent.IDENTIFIER.exactly().systemAndCode(fisProperties.getFhir().getConsent().getCodeSystem(), c2sConsent.getConsentReferenceId()))
                .execute();

        return fhirContext.newJsonParser().setPrettyPrint(true)
                .encodeResourceToString(fhirConsent);

    }

    public Consent createFhirConsent(ConsentDto c2sConsent, PatientDto patientDto) {
        Consent fhirConsent = new Consent();

        // Set the id as a concatenated "OID.consentId"
        fhirConsent.setId(new IdType(c2sConsent.getId()));

        // Set patient reference and add patient as contained resource
        if (fisProperties.getFhir().getConsent().isPatientReference()) {
            String patientResourceId = patientService.getPatientResourceId(patientDto.getIdentifiers().get().stream().filter(identifier -> identifier.getValue().equalsIgnoreCase(patientDto.getMrn())).findFirst().get().getSystem(), patientDto.getMrn());
            fhirConsent.getPatient().setReference("Patient/" + patientResourceId);

            // Consent signature details
            Reference consentSignature = new Reference();
            consentSignature.setDisplay(patientDto.getFirstName() + " " + patientDto.getLastName());
            consentSignature.setReference("Patient/" + patientResourceId);
            fhirConsent.getConsentingParty().add(consentSignature);

        } else {
            Patient fhirPatient = patientService.createFhirPatient(patientDto);
            fhirConsent.getPatient().setReference("#" + fhirPatient.getId());
            fhirConsent.getContained().add(fhirPatient);

            // Consent signature details
            Reference consentSignature = new Reference();
            consentSignature.setDisplay(fhirPatient.getNameFirstRep().getNameAsSingleString());
            consentSignature.setReference("#" + patientDto.getId());
            fhirConsent.getConsentingParty().add(consentSignature);
        }

        // Set identifier for this consent
        fhirConsent.getIdentifier().setSystem(fisProperties.getFhir().getConsent().getCodeSystem()).setValue(c2sConsent.getConsentReferenceId());

        // Consent status
        fhirConsent.setStatus(Consent.ConsentState.ACTIVE);

        // Set category
        CodeableConcept categoryConcept = new CodeableConcept();

        categoryConcept.addCoding(
                new Coding().setCode(V3ActCode.IDSCL.toCode())
                        .setSystem(V3ActCode.IDSCL.getSystem())
                        .setDisplay(V3ActCode.IDSCL.getDisplay())
        );
        fhirConsent.getCategory().add(categoryConcept);

        // Set terms of consent and intended recipient(s)
        fhirConsent.getPeriod().setStart(
                Date.from(c2sConsent.getStartDate().atZone(ZoneId.systemDefault()).toInstant())
        );
        fhirConsent.getPeriod().setEnd(
                Date.from(c2sConsent.getEndDate().atZone(ZoneId.systemDefault()).toInstant())
        );

        // Consent sign time
        fhirConsent.setDateTime(new Date());

        // Check "From" Organizations
        List<Organization> sourceOrganizationList = null;
        if (c2sConsent.getConsentAttestation().getFromOrganizations().size() > 0) {
            sourceOrganizationList = getFHIROrganizationProviderList(
                    c2sConsent.getConsentAttestation().getFromOrganizations());

            if (sourceOrganizationList != null && sourceOrganizationList.size() > 0) {
                sourceOrganizationList.stream().forEach((org) -> {
                    fhirConsent.getContained().add(org);
                     fhirConsent.getActor().add(getAuthorizedToShareActorComponent(org.getId()));
                });
            }
        }
        // Check "From" Individual Providers
        List<Practitioner> sourcePractitionerList = null;
        if (c2sConsent.getConsentAttestation().getFromPractitioners().size() > 0) {
            sourcePractitionerList = getFHIRIndividualProviderList(
                    c2sConsent.getConsentAttestation().getFromPractitioners());

            if (sourcePractitionerList != null && sourcePractitionerList.size() > 0) {
                sourcePractitionerList.stream().forEach((individualProvider) -> {
                    fhirConsent.getContained().add(individualProvider);
                    fhirConsent.getActor().add(getAuthorizedToShareActorComponent(individualProvider.getId()));
                });
            }
        }

        // Check "To" Organizations and set them as Actor
        List<Organization> recipientOrganizationList = null;
        if (c2sConsent.getConsentAttestation().getToOrganizations().size() > 0) {
            recipientOrganizationList = getFHIROrganizationProviderList(
                    c2sConsent.getConsentAttestation().getToOrganizations());

            if (recipientOrganizationList != null && recipientOrganizationList.size() > 0) {
                recipientOrganizationList.stream().forEach((org) -> {
                    fhirConsent.getContained().add(org);
                    fhirConsent.getActor().add(getSharingWithActorComponent(org.getId()));
                });
            }
        }

        // Check "To" Individual Providers and set them as Actor
        List<Practitioner> recipientPractitionerList = null;
        if (c2sConsent.getConsentAttestation().getToPractitioners().size() > 0) {
            recipientPractitionerList = getFHIRIndividualProviderList(
                    c2sConsent.getConsentAttestation().getToPractitioners());

            if (recipientPractitionerList != null && recipientPractitionerList.size() > 0) {
                recipientPractitionerList.stream().forEach((individualProvider) -> {
                    fhirConsent.getContained().add(individualProvider);
                    fhirConsent.getActor().add(getSharingWithActorComponent(individualProvider.getId()));
                });
            }
        }

        // Set Security Coding Label as Restricted
        List<Coding> securityCodingList = new ArrayList<>();
        securityCodingList.add(new Coding(Composition.DocumentConfidentiality.R.getSystem(),
                                            Composition.DocumentConfidentiality.R.toCode(),
                                            Composition.DocumentConfidentiality.R.getDisplay()));
        fhirConsent.setSecurityLabel(securityCodingList);

        // Set Purpose
        for (PurposeDto purpose : c2sConsent.getSharePurposes()) {
            String pou = purpose.getIdentifier().getValue();
            Coding coding = new Coding(purpose.getIdentifier().getSystem(), pou, purpose.getDisplay());
            fhirConsent.getPurpose().add(coding);
        }

        // Set Exempt portion
        // First, get "share" categories from consent
        List<String> shareCodes = c2sConsent.getShareSensitivityCategories()
                .stream()
                .map(codes -> codes.getIdentifier().getValue())
                .collect(Collectors.toList());

        List<Coding> includeCodingList = new ArrayList<>();

        // Get all sensitive categories from vss
        List<ValueSetCategoryDto> allSensitiveCategories = vssClient.getValueSetCategories();

        // Go over the full list of Sensitive Categories and add obligation as exclusions
        for (ValueSetCategoryDto valueSetCategoryDto : allSensitiveCategories) {
            if (shareCodes.contains(valueSetCategoryDto.getCode())) {
                String systemUrl = valueSetCategoryDto.getSystem();
                String code = valueSetCategoryDto.getCode();
                if (!(
                        code.equalsIgnoreCase(V3ActCode.ETH.toCode())
                                || code.equalsIgnoreCase(V3ActCode.PSY.toCode())
                                || code.equalsIgnoreCase(V3ActCode.SEX.toCode())
                                || code.equalsIgnoreCase(V3ActCode.HIV.toCode())
                )) {
                    systemUrl = fisProperties.getFhir().getConsent().getCodeSystem();
                }
                // Include it
                includeCodingList.add(new Coding(systemUrl, code, valueSetCategoryDto.getName()));
            }
        }

        Consent.ExceptComponent exceptComponent = new Consent.ExceptComponent();

        // List of included Sensitive policy codes
        exceptComponent.setSecurityLabel(includeCodingList);
        exceptComponent.setType(Consent.ConsentExceptType.PERMIT);

        fhirConsent.setExcept(Collections.singletonList(exceptComponent));

        // Log FHIRConsent into json and xml format in debug mode
        logFHIRConsent(fhirConsent);

        return fhirConsent;
    }

    private List<Organization> getFHIROrganizationProviderList(List<OrganizationDto> organizationList) {
        List<Organization> fhirOrganizationList = new ArrayList<>();

        organizationList.stream().forEach((OrganizationDto tempOrganization) -> {
            Organization sourceOrganizationResource = new Organization();
            String orgNpi = tempOrganization.getProvider().getIdentifier().getValue();
            sourceOrganizationResource.setId(new IdType(orgNpi));
            sourceOrganizationResource.addIdentifier().setSystem(tempOrganization.getProvider().getIdentifier().getSystem()).setValue(orgNpi);
            // Set the name element
            sourceOrganizationResource.setName(tempOrganization.getName());
            // Set the address
            sourceOrganizationResource.addAddress().addLine(tempOrganization.getAddress().getLine1())
                    .setCity(tempOrganization.getAddress().getCity())
                    .setState(tempOrganization.getAddress().getStateCode())
                    .setPostalCode(tempOrganization.getAddress().getPostalCode());
            fhirOrganizationList.add(sourceOrganizationResource);
        });

        return fhirOrganizationList;
    }

    private List<Practitioner> getFHIRIndividualProviderList(List<PractitionerDto> practitionerList) {
        List<Practitioner> fhirPractitionerList = new ArrayList<>();

        practitionerList.stream().forEach((PractitionerDto tempPractitioner) -> {
            Practitioner sourcePractitionerResource = new Practitioner();
            String practitionerNPI = tempPractitioner.getProvider().getIdentifier().getValue();
            sourcePractitionerResource.setId(new IdType(practitionerNPI));
            sourcePractitionerResource.addIdentifier().setSystem(tempPractitioner.getProvider().getIdentifier().getSystem()).setValue(practitionerNPI);
            // Set the name element
            HumanName indName = new HumanName();
            indName.setFamily(tempPractitioner.getLastName());
            indName.addGiven(tempPractitioner.getFirstName());
            sourcePractitionerResource.addName(indName);
            // Set the address
            sourcePractitionerResource.addAddress().addLine(tempPractitioner.getAddress().getLine1())
                    .setCity(tempPractitioner.getAddress().getCity())
                    .setState(tempPractitioner.getAddress().getStateCode())
                    .setPostalCode(tempPractitioner.getAddress().getPostalCode());
            fhirPractitionerList.add(sourcePractitionerResource);
        });

        return fhirPractitionerList;
    }

    private Consent.ConsentActorComponent getAuthorizedToShareActorComponent(String orgOrIndividualProviderId){
        //"From" providers(Role = INF)
        Consent.ConsentActorComponent actor = new Consent.ConsentActorComponent();
        actor.setReference(new Reference().setReference("#" + orgOrIndividualProviderId));
        actor.setRole(new CodeableConcept().addCoding(new Coding(V3ParticipationType.INF.getSystem(),
                V3ParticipationType.INF.toCode(),
                V3ParticipationType.INF.getDisplay())));
        return actor;
    }

    private Consent.ConsentActorComponent getSharingWithActorComponent(String orgOrIndividualProviderId){
        //"To" providers(Role = IRCP)
        Consent.ConsentActorComponent actor = new Consent.ConsentActorComponent();
        actor.setReference(new Reference().setReference("#" + orgOrIndividualProviderId));
        actor.setRole(new CodeableConcept().addCoding(new Coding(V3ParticipationType.IRCP.getSystem(),
                V3ParticipationType.IRCP.toCode(),
                V3ParticipationType.IRCP.getDisplay())));
        return actor;
    }

    private void logFHIRConsent(Consent fhirConsent) {
        log.debug(fhirContext.newXmlParser().setPrettyPrint(true)
                .encodeResourceToString(fhirConsent));
        log.debug(fhirContext.newJsonParser().setPrettyPrint(true)
                .encodeResourceToString(fhirConsent));
    }


    public String searchFhirConsents(String patientMrnSystem,  String patientMrn){
        String patientResourceId =patientService.getPatientResourceId(patientMrnSystem,patientMrn);
        String dateToday = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        Bundle consentSearchResponse = consentFhirClient.search()
                .forResource(Consent.class)
                .where(new ReferenceClientParam("patient")
                        .hasId(patientResourceId))
                .where(new TokenClientParam("status").exactly().code("active"))
                .where(new DateClientParam("period").afterOrEquals().second(dateToday))
                .where(new DateClientParam("period").beforeOrEquals().second(dateToday))
                .returnBundle(Bundle.class)
                .execute();
        return fhirJsonParser.setPrettyPrint(true).encodeResourceToString(consentSearchResponse);
    }
}
