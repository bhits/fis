package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PractitionerDto {

    private ProviderDto provider;

    @NotBlank
    private String firstName;
    private String middleName;
    @NotBlank
    private String lastName;

    private AddressDto address;

    private String phoneNumber;

}
