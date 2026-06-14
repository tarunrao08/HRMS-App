package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;
import com.hrms.payroll.entity.PayrollComponent;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PayrollComponentMapper {

    PayrollComponent toEntity(PayrollComponentRequest request);

    PayrollComponentResponse toResponse(PayrollComponent component);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget PayrollComponent target, PayrollComponentRequest request);
}
