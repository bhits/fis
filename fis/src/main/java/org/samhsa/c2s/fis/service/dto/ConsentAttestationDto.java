package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentAttestationDto {

    private Date attestedDate;

    private String attestedBy;

    private Boolean attestedByPatient;

    @NotNull
    private ConsentAttestationTermDto consentAttestationTerm;

    private List<PractitionerDto> fromPractitioners = new ArrayList<>();

    private List<OrganizationDto> fromOrganizations = new ArrayList<>();

    private List<PractitionerDto> toPractitioners = new ArrayList<>();

    private List<OrganizationDto> toOrganizations = new ArrayList<>();
}
