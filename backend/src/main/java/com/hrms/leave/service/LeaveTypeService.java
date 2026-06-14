package com.hrms.leave.service;

import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.dto.LeaveTypeResponse;

import java.util.List;
import java.util.UUID;

public interface LeaveTypeService {

    LeaveTypeResponse create(LeaveTypeRequest request);

    LeaveTypeResponse getById(UUID id);

    List<LeaveTypeResponse> getAll();

    List<LeaveTypeResponse> getAllActive();

    LeaveTypeResponse update(UUID id, LeaveTypeRequest request);

    void delete(UUID id);
}
