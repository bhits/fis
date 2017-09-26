package org.samhsa.c2s.fis.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class IdentifierDto {
    @NotBlank
    private String value;
    @NotBlank
    private String system;
}
