package com.hrms.attendance.controller;

import com.hrms.attendance.dto.*;
import com.hrms.attendance.service.AttendanceService;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<AttendanceRecordResponse>>> getAll(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attendanceDate"));
        return ResponseEntity.ok(ApiResponse.paginated(
                attendanceService.getAll(employeeId, fromDate, toDate, status, pageable)));
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<List<AttendanceMonthlySummaryResponse>>> getAllMonthlySummaries(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAllMonthlySummaries(year, month)));
    }

    @PostMapping("/punch-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> punchIn(
            @RequestParam UUID employeeId,
            @RequestBody(required = false) PunchRequest request) {
        PunchRequest req = request != null ? request : new PunchRequest();
        AttendanceRecordResponse response = attendanceService.punchIn(employeeId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Punch-in recorded", response));
    }

    @PostMapping("/punch-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> punchOut(
            @RequestParam UUID employeeId,
            @RequestBody(required = false) PunchRequest request) {
        PunchRequest req = request != null ? request : new PunchRequest();
        AttendanceRecordResponse response = attendanceService.punchOut(employeeId, req);
        return ResponseEntity.ok(ApiResponse.success("Punch-out recorded", response));
    }

    @PostMapping("/{id}/regularize")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> regularize(
            @PathVariable UUID id,
            @RequestParam UUID regularizedBy,
            @Valid @RequestBody RegularizationRequest request) {
        AttendanceRecordResponse response = attendanceService.regularize(id, regularizedBy, request);
        return ResponseEntity.ok(ApiResponse.success("Attendance regularized", response));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<PageableResponse<AttendanceRecordResponse>>> getByEmployee(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "attendanceDate"));
        PageableResponse<AttendanceRecordResponse> result = attendanceService.getByEmployee(employeeId, from, to,
                pageable);
        return ResponseEntity.ok(ApiResponse.paginated(result));
    }

    @GetMapping("/employee/{employeeId}/monthly-summary")
    public ResponseEntity<ApiResponse<AttendanceMonthlySummaryResponse>> getMonthlySummary(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getMonthlySummary(employeeId, year, month)));
    }

    @GetMapping("/employee/{employeeId}/monthly-summaries")
    public ResponseEntity<ApiResponse<List<AttendanceMonthlySummaryResponse>>> getEmployeeSummaries(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getEmployeeSummaries(employeeId)));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TodayAttendanceResponse>>> getTodayAttendance() {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getTodayAttendance()));
    }

    @PostMapping("/mark")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<TodayAttendanceResponse>> markAttendance(
            @Valid @RequestBody MarkAttendanceRequest request) {
        TodayAttendanceResponse response = attendanceService.markAttendance(request);
        return ResponseEntity.ok(ApiResponse.success("Attendance marked", response));
    }

    @PostMapping("/shifts/assign")
    @PreAuthorize("hasRole('HR_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeShiftResponse>> assignShift(
            @Valid @RequestBody EmployeeShiftRequest request) {
        EmployeeShiftResponse response = attendanceService.assignShift(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift assigned to employee", response));
    }

    @GetMapping("/shifts/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<EmployeeShiftResponse>>> getEmployeeShifts(
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getEmployeeShifts(employeeId)));
    }
}
