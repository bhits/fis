package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "consent")
@EqualsAndHashCode(exclude = "consent")
public class ConsentRevocationDto {
    private Long id;

    private Date revokedDate;

    private String revokedBy;

    private Boolean revokedByPatient;

    private ConsentRevocationTermDto consentRevocationTerm;

}
