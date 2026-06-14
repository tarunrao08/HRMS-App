package com.hrms.payroll.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.payroll.dto.PayslipResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PayslipService {

    PayslipResponse getByEmployeeAndPeriod(UUID employeeId, int year, int month);

    PageableResponse<PayslipResponse> getByEmployee(UUID employeeId, Pageable pageable);

    PayslipResponse publish(UUID payslipId);

    List<PayslipResponse> getByRun(UUID runId);

    java.util.Optional<PayslipResponse> getLatestPublished(UUID employeeId);

    PageableResponse<PayslipResponse> getMyPayslips(UUID employeeId, Pageable pageable);
}
