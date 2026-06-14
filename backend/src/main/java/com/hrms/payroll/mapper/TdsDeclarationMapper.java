package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.TdsDeclarationRequest;
import com.hrms.payroll.dto.TdsDeclarationResponse;
import com.hrms.payroll.entity.TdsDeclaration;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TdsDeclarationMapper {

    @Mapping(target = "employee", ignore = true)
    TdsDeclaration toEntity(TdsDeclarationRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(expression = "java(declaration.getEmployee().getFirstName() + \" \" + declaration.getEmployee().getLastName())", target = "employeeName")
    TdsDeclarationResponse toResponse(TdsDeclaration declaration);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "employee", ignore = true)
    void updateEntity(@MappingTarget TdsDeclaration target, TdsDeclarationRequest request);
}
