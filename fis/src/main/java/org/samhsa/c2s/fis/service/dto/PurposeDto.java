package org.samhsa.c2s.fis.service.dto;

import lombok.Data;

@Data
public class PurposeDto {

    private IdentifierDto identifier;

    private String display;

    private String description;
}
