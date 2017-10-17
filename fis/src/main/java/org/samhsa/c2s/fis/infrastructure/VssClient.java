package org.samhsa.c2s.fis.infrastructure;

import org.samhsa.c2s.fis.service.dto.ValueSetCategoryDto;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "vss")
public interface VssClient {

    @RequestMapping(value = "/valueSetCategories", method = RequestMethod.GET)
    List<ValueSetCategoryDto> getValueSetCategories();
}
