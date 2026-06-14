package com.hrms.payroll.service;

import com.hrms.common.dto.PageableResponse;
import com.hrms.payroll.dto.InitiatePayrollRunRequest;
import com.hrms.payroll.dto.PayrollRunResponse;
import com.hrms.payroll.dto.PayslipResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PayrollRunService {

    PayrollRunResponse initiate(InitiatePayrollRunRequest request, UUID processedBy);

    PayrollRunResponse process(UUID runId);

    PayrollRunResponse approve(UUID runId, UUID approvedBy);

    PayrollRunResponse reject(UUID runId);

    PayrollRunResponse disburse(UUID runId, UUID disbursedBy);

    PayrollRunResponse getById(UUID id);

    PageableResponse<PayrollRunResponse> getAll(Pageable pageable);

    PayslipResponse generatePayslipForEmployee(UUID employeeId, int month, int year, UUID requestedBy);
}
