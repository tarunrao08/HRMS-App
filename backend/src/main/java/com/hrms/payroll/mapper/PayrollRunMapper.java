package com.hrms.payroll.mapper;

import com.hrms.payroll.dto.PayrollRunResponse;
import com.hrms.payroll.entity.PayrollRun;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayrollRunMapper {

    PayrollRunResponse toResponse(PayrollRun payrollRun);
}
