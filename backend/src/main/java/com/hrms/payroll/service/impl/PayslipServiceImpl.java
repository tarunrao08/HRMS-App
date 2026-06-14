package com.hrms.payroll.service.impl;

import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.payroll.dto.PayslipResponse;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.mapper.PayslipMapper;
import com.hrms.payroll.repository.PayslipRepository;
import com.hrms.payroll.service.PayslipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayslipServiceImpl implements PayslipService {

    private final PayslipRepository payslipRepository;
    private final PayslipMapper payslipMapper;

    @Override
    @Transactional(readOnly = true)
    public PayslipResponse getByEmployeeAndPeriod(UUID employeeId, int year, int month) {
        Payslip payslip = payslipRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "employeeId/year/month",
                        employeeId + "/" + year + "/" + month));
        return payslipMapper.toResponse(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<PayslipResponse> getByEmployee(UUID employeeId, Pageable pageable) {
        Page<PayslipResponse> page = payslipRepository.findByEmployeeId(employeeId, pageable)
                .map(payslipMapper::toResponse);
        return PageableResponse.of(page);
    }

    @Override
    public PayslipResponse publish(UUID payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", payslipId.toString()));
        payslip.setPublished(true);
        payslip.setPublishedAt(Instant.now());
        payslip = payslipRepository.save(payslip);
        log.info("Published payslip: {}", payslipId);
        return payslipMapper.toResponse(payslip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipResponse> getByRun(UUID runId) {
        return payslipRepository.findByPayrollRunId(runId).stream()
                .map(payslipMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PayslipResponse> getLatestPublished(UUID employeeId) {
        return payslipRepository
                .findLatestVisibleForEmployee(employeeId, PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .map(payslipMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<PayslipResponse> getMyPayslips(UUID employeeId, Pageable pageable) {
        Page<PayslipResponse> page = payslipRepository
                .findLatestVisibleForEmployee(employeeId, pageable)
                .map(payslipMapper::toResponse);
        return PageableResponse.of(page);
    }
}
