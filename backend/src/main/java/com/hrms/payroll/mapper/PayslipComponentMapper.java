package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.PayslipComponentResponse;
import com.hrms.payroll.entity.PayslipComponent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PayslipComponentMapper {

    @Mapping(source = "payrollComponent.id",           target = "payrollComponentId")
    @Mapping(source = "payrollComponent.code",          target = "code")
    @Mapping(source = "payrollComponent.name",          target = "name")
    @Mapping(source = "payrollComponent.componentType", target = "componentType")
    @Mapping(source = "payrollComponent.displayOrder",  target = "displayOrder")
    PayslipComponentResponse toResponse(PayslipComponent component);

    List<PayslipComponentResponse> toResponseList(List<PayslipComponent> components);
}
