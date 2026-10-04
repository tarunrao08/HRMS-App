package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;
import com.hrms.payroll.entity.PayrollComponent;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PayrollComponentMapper {

    // percentageOfComponent is resolved from percentageOfComponentId by the service (needs a repository lookup)
    @Mapping(target = "percentageOfComponent", ignore = true)
    PayrollComponent toEntity(PayrollComponentRequest request);

    @Mapping(source = "percentageOfComponent.id",   target = "percentageOfComponentId")
    @Mapping(source = "percentageOfComponent.code", target = "percentageOfComponentCode")
    PayrollComponentResponse toResponse(PayrollComponent component);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "percentageOfComponent", ignore = true)
    void updateEntity(@MappingTarget PayrollComponent target, PayrollComponentRequest request);
}
