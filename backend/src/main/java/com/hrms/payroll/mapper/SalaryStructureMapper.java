package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.SalaryStructureResponse;
import com.hrms.payroll.entity.SalaryStructure;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SalaryStructureMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "grossSalary", target = "monthlyGross")
    @Mapping(expression = "java(structure.getEmployee().getFirstName() + \" \" + structure.getEmployee().getLastName())",
             target = "employeeName")
    SalaryStructureResponse toResponse(SalaryStructure structure);
}
