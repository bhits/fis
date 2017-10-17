package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@ToString(exclude = "patient")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentDto {

    private Long id;

    private Date createdDate;

    private String createdBy;

    private Boolean createdByPatient;

    private List<ProviderDto> fromProviders = new ArrayList<>();

    private List<ProviderDto> toProviders = new ArrayList<>();

    private ConsentAttestationDto consentAttestation;

    private ConsentRevocationDto consentRevocation;

    private List<SensitivityCategoryDto> shareSensitivityCategories = new ArrayList<>();

    private List<PurposeDto> sharePurposes = new ArrayList<>();

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @NotNull
    private String consentReferenceId;

    @NotNull
    private boolean deleted = false;

}
