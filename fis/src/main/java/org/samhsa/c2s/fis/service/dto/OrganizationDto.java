package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"consentAttestationFrom", "consentAttestationTo"})
@EqualsAndHashCode(exclude = {"consentAttestationFrom", "consentAttestationTo"})
public class OrganizationDto {
    private Long id;

    private ProviderDto provider;

    @NotBlank
    private String name;

    private AddressDto address;

    private String phoneNumber;

}
