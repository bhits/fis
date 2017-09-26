package org.samhsa.c2s.fis.web;

import org.samhsa.c2s.fis.service.PatientService;
import org.samhsa.c2s.fis.service.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/patients")
public class PatientRestController {

    @Autowired
    private PatientService patientService;

    @PostMapping
    void addPatient(@Valid @RequestBody UserDto userDto) {
        patientService.publishFhirPatient(userDto);
    }

    @GetMapping("/{mrn}/resources")
    String getPatientFhirResource(@PathVariable String mrn){
        return patientService.getPatientFhirResource(mrn);
    }
}
