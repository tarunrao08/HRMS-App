package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.PayslipResponse;
import com.hrms.payroll.entity.Payslip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayslipMapper {

    @Mapping(source = "payrollRun.id",        target = "payrollRunId")
    @Mapping(source = "employee.id",           target = "employeeId")
    @Mapping(source = "employee.employeeCode", target = "employeeCode")
    @Mapping(expression = "java(payslip.getEmployee().getFirstName() + \" \" + payslip.getEmployee().getLastName())",
             target = "employeeName")
    PayslipResponse toResponse(Payslip payslip);
}
