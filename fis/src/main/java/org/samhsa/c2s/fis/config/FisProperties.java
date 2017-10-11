package org.samhsa.c2s.fis.config;

import ca.uhn.fhir.rest.api.EncodingEnum;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "c2s.fis")
@Data
public class FisProperties {

    @Valid
    private Ssn ssn;

    @Valid
    private Mrn mrn;


    @NotNull
    @Valid
    private Fhir fhir;

    @Data
    public static class Fhir {

        private Publish publish;

        @Data
        public static class Publish {
            @NotBlank
            private boolean enabled;
            @NotBlank
            private ServerUrl serverUrl;
            @NotBlank
            private String clientSocketTimeoutInMs;
            @NotBlank
            private boolean useCreateForUpdate = false;
            @NotNull
            private EncodingEnum encoding = EncodingEnum.JSON;
        }

    }

    @Data
    public static class ServerUrl {

        @NotBlank
        private String resource;

        private String patient;
    }

    @Data
    public static class Identifier {
        @NotBlank
        private String codeSystem;

        @NotBlank
        private String codeSystemOID;
    }

    @Data
    public static class Mrn extends Identifier {
    }


    @Data
    public static class Ssn extends Identifier {
    }


}
