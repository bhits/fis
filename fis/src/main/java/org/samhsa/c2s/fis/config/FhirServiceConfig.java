package org.samhsa.c2s.fis.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class FhirServiceConfig {

    @Autowired
    private FisProperties fisProperties;

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = FhirContext.forDstu3();
        fhirContext.getRestfulClientFactory().setSocketTimeout(Integer.parseInt(fisProperties.getFhir().getPublish().getClientSocketTimeoutInMs()));
        return fhirContext;
    }

    @Bean
    public Map<Class<? extends Resource>, IGenericClient> fhirClients() {
        final Map<Class<? extends Resource>, IGenericClient> fhirClients = new HashMap<>();
        if(fisProperties.getFhir().getPublish().getServerUrl().getPatient()!=null)
            fhirClients.put(Patient.class, fhirContext().newRestfulGenericClient(fisProperties.getFhir().getPublish().getServerUrl().getPatient()));
        fhirClients.put(Resource.class, fhirContext().newRestfulGenericClient(fisProperties.getFhir().getPublish().getServerUrl().getResource()));
        return fhirClients;
    }

    @Bean
    public IParser fhirXmlParser() {
        return fhirContext().newXmlParser();
    }

    @Bean
    public IParser fhirJsonParser() {
        return fhirContext().newJsonParser();
    }

    @Bean
    public FhirValidator fhirValidator() {
        return fhirContext().newValidator();
    }


}
