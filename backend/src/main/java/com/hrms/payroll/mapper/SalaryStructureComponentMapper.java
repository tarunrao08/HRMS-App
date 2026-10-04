package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.SalaryStructureComponentResponse;
import com.hrms.payroll.entity.SalaryStructureComponent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SalaryStructureComponentMapper {

    @Mapping(source = "payrollComponent.id",           target = "payrollComponentId")
    @Mapping(source = "payrollComponent.code",          target = "code")
    @Mapping(source = "payrollComponent.name",          target = "name")
    @Mapping(source = "payrollComponent.componentType", target = "componentType")
    @Mapping(source = "payrollComponent.displayOrder",  target = "displayOrder")
    SalaryStructureComponentResponse toResponse(SalaryStructureComponent component);

    List<SalaryStructureComponentResponse> toResponseList(List<SalaryStructureComponent> components);
}
