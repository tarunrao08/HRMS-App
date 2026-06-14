package com.hrms.attendance.service;

import com.hrms.attendance.dto.ShiftRequest;
import com.hrms.attendance.dto.ShiftResponse;

import java.util.List;
import java.util.UUID;

public interface ShiftService {

    ShiftResponse create(ShiftRequest request);

    ShiftResponse getById(UUID id);

    List<ShiftResponse> getAll();

    ShiftResponse update(UUID id, ShiftRequest request);

    void delete(UUID id);

    ShiftResponse toggleActive(UUID id);
}
