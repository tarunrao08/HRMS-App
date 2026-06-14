package com.hrms.attendance.service;

import com.hrms.attendance.dto.*;
import com.hrms.common.dto.PageableResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceRecordResponse punchIn(UUID employeeId, PunchRequest request);

    AttendanceRecordResponse punchOut(UUID employeeId, PunchRequest request);

    AttendanceRecordResponse regularize(UUID recordId, UUID regularizedBy, RegularizationRequest request);

    PageableResponse<AttendanceRecordResponse> getByEmployee(UUID employeeId, LocalDate from, LocalDate to,
                                                             Pageable pageable);

    AttendanceMonthlySummaryResponse getMonthlySummary(UUID employeeId, int year, int month);

    List<AttendanceMonthlySummaryResponse> getEmployeeSummaries(UUID employeeId);

    PageableResponse<AttendanceRecordResponse> getAll(UUID employeeId, LocalDate fromDate, LocalDate toDate,
                                                      String status, Pageable pageable);

    List<AttendanceMonthlySummaryResponse> getAllMonthlySummaries(int year, int month);

    EmployeeShiftResponse assignShift(EmployeeShiftRequest request);

    List<EmployeeShiftResponse> getEmployeeShifts(UUID employeeId);

    List<TodayAttendanceResponse> getTodayAttendance();

    TodayAttendanceResponse markAttendance(MarkAttendanceRequest request);
}
