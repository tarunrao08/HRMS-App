package com.hrms.payroll.service;

import com.hrms.payroll.dto.PayrollComponentRequest;
import com.hrms.payroll.dto.PayrollComponentResponse;

import java.util.List;
import java.util.UUID;

public interface PayrollComponentService {

    PayrollComponentResponse create(PayrollComponentRequest request);

    PayrollComponentResponse getById(UUID id);

    List<PayrollComponentResponse> getAll();

    List<PayrollComponentResponse> getActive();

    PayrollComponentResponse update(UUID id, PayrollComponentRequest request);

    void delete(UUID id);
}
