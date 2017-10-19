package org.samhsa.c2s.fis.service.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ConsentRevocationTermDto {
    private Long id;

    @Size(max = 20000)
    private String text;

}
