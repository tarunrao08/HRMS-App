package com.hrms.attendance.service.impl;

import com.hrms.attendance.dto.*;
import com.hrms.attendance.entity.AttendanceMonthlySummary;
import com.hrms.attendance.entity.AttendanceRecord;
import com.hrms.attendance.entity.EmployeeShift;
import com.hrms.attendance.entity.Shift;
import com.hrms.attendance.enums.AttendanceStatus;
import com.hrms.attendance.mapper.AttendanceMapper;
import com.hrms.attendance.repository.*;
import com.hrms.attendance.service.AttendanceService;
import com.hrms.common.dto.PageableResponse;
import org.springframework.data.jpa.domain.Specification;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceMonthlySummaryRepository monthlySummaryRepository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;

    @Override
    public AttendanceRecordResponse punchIn(UUID employeeId, PunchRequest request) {
        LocalDate today = LocalDate.now();

        if (attendanceRecordRepository.findByEmployeeIdAndAttendanceDate(employeeId, today).isPresent()) {
            throw new ValidationException("Already punched in for today");
        }

        Employee employee = findEmployeeOrThrow(employeeId);

        List<EmployeeShift> activeShifts = employeeShiftRepository.findActiveShiftForEmployee(employeeId, today);
        Shift shift = activeShifts.isEmpty() ? null : activeShifts.get(0).getShift();

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .attendanceDate(today)
                .punchIn(Instant.now())
                .status(AttendanceStatus.PRESENT)
                .shift(shift)
                .punchInLocation(request.getLocation())
                .punchInIp(request.getIp())
                .remarks(request.getRemarks())
                .build();

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Punch-in recorded for employee={} date={}", employeeId, today);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public AttendanceRecordResponse punchOut(UUID employeeId, PunchRequest request) {
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "employeeId+date",
                        employeeId + "/" + today));

        if (record.getPunchOut() != null) {
            throw new ValidationException("Already punched out for today");
        }

        Instant punchOut = Instant.now();
        record.setPunchOut(punchOut);
        record.setPunchOutLocation(request.getLocation());
        record.setPunchOutIp(request.getIp());

        if (record.getPunchIn() != null) {
            long seconds = punchOut.getEpochSecond() - record.getPunchIn().getEpochSecond();
            BigDecimal hours = BigDecimal.valueOf(seconds)
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
            record.setWorkingHours(hours);
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Punch-out recorded for employee={} date={}", employeeId, today);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public AttendanceRecordResponse regularize(UUID recordId, UUID regularizedBy,
                                               RegularizationRequest request) {
        AttendanceRecord record = attendanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceRecord", "id", recordId.toString()));

        record.setStatus(request.getStatus());
        record.setRegularizationReason(request.getReason());
        record.setRegularized(true);
        record.setRegularizedBy(regularizedBy);
        record.setRegularizedAt(Instant.now());

        if (request.getPunchIn() != null) {
            record.setPunchIn(request.getPunchIn());
        }
        if (request.getPunchOut() != null) {
            record.setPunchOut(request.getPunchOut());
        }

        if (record.getPunchIn() != null && record.getPunchOut() != null) {
            long seconds = record.getPunchOut().getEpochSecond() - record.getPunchIn().getEpochSecond();
            BigDecimal hours = BigDecimal.valueOf(seconds)
                    .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
            record.setWorkingHours(hours);
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Regularized attendance record id={} by={}", recordId, regularizedBy);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<AttendanceRecordResponse> getByEmployee(UUID employeeId, LocalDate from,
                                                                     LocalDate to, Pageable pageable) {
        if (from != null && to != null) {
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(employeeId, from, to);
            List<AttendanceRecordResponse> content = records.stream()
                    .map(attendanceMapper::toResponse)
                    .toList();
            return PageableResponse.<AttendanceRecordResponse>builder()
                    .content(content)
                    .page(0)
                    .size(content.size())
                    .totalElements(content.size())
                    .totalPages(1)
                    .first(true)
                    .last(true)
                    .build();
        }

        Page<AttendanceRecord> page = attendanceRecordRepository.findByEmployeeId(employeeId, pageable);
        Page<AttendanceRecordResponse> mapped = page.map(attendanceMapper::toResponse);
        return PageableResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = ResourceNotFoundException.class)
    public AttendanceMonthlySummaryResponse getMonthlySummary(UUID employeeId, int year, int month) {
        AttendanceMonthlySummary summary = monthlySummaryRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceMonthlySummary",
                        "employeeId+year+month", employeeId + "/" + year + "/" + month));
        return attendanceMapper.toSummaryResponse(summary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceMonthlySummaryResponse> getEmployeeSummaries(UUID employeeId) {
        return monthlySummaryRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId)
                .stream()
                .map(attendanceMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<AttendanceRecordResponse> getAll(UUID employeeId, LocalDate fromDate, LocalDate toDate,
                                                             String status, Pageable pageable) {
        Specification<AttendanceRecord> spec = Specification.where(null);
        if (employeeId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (fromDate != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("attendanceDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("attendanceDate"), toDate));
        }
        if (status != null && !status.isBlank()) {
            try {
                AttendanceStatus s = AttendanceStatus.valueOf(status);
                spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), s));
            } catch (IllegalArgumentException ignored) {}
        }
        Page<AttendanceRecordResponse> page = attendanceRecordRepository.findAll(spec, pageable)
                .map(attendanceMapper::toResponse);
        return PageableResponse.of(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceMonthlySummaryResponse> getAllMonthlySummaries(int year, int month) {
        return monthlySummaryRepository.findByYearAndMonth(year, month).stream()
                .map(attendanceMapper::toSummaryResponse)
                .toList();
    }

    @Override
    public EmployeeShiftResponse assignShift(EmployeeShiftRequest request) {
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());
        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id",
                        request.getShiftId().toString()));

        EmployeeShift employeeShift = EmployeeShift.builder()
                .employee(employee)
                .shift(shift)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        EmployeeShift saved = employeeShiftRepository.save(employeeShift);
        log.info("Assigned shift={} to employee={} from={}", shift.getId(), employee.getId(),
                request.getEffectiveFrom());
        return attendanceMapper.toEmployeeShiftResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getEmployeeShifts(UUID employeeId) {
        return employeeShiftRepository.findByEmployeeId(employeeId)
                .stream()
                .map(attendanceMapper::toEmployeeShiftResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayAttendanceResponse> getTodayAttendance() {
        LocalDate today = LocalDate.now();
        List<Employee> activeEmployees = employeeRepository
                .findByEmploymentStatusOrderByFirstNameAscLastNameAsc(EmploymentStatus.ACTIVE);

        Map<UUID, AttendanceRecord> recordsByEmployee = attendanceRecordRepository
                .findByAttendanceDate(today)
                .stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), r -> r));

        return activeEmployees.stream().map(emp -> {
            AttendanceRecord rec = recordsByEmployee.get(emp.getId());
            return TodayAttendanceResponse.builder()
                    .employeeId(emp.getId())
                    .employeeName(emp.getFirstName() + " " + emp.getLastName())
                    .employeeCode(emp.getEmployeeCode())
                    .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                    .designationTitle(emp.getDesignation() != null ? emp.getDesignation().getName() : null)
                    .status(rec != null ? rec.getStatus() : null)
                    .recordId(rec != null ? rec.getId() : null)
                    .build();
        }).toList();
    }

    @Override
    public TodayAttendanceResponse markAttendance(MarkAttendanceRequest request) {
        LocalDate today = LocalDate.now();
        Employee employee = findEmployeeOrThrow(request.getEmployeeId());

        AttendanceRecord record = attendanceRecordRepository
                .findByEmployeeIdAndAttendanceDate(request.getEmployeeId(), today)
                .orElse(null);

        if (record == null) {
            record = AttendanceRecord.builder()
                    .employee(employee)
                    .attendanceDate(today)
                    .status(request.getStatus())
                    .build();
        } else {
            record.setStatus(request.getStatus());
        }

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        log.info("Marked attendance for employee={} date={} status={}", employee.getId(), today, request.getStatus());

        return TodayAttendanceResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .employeeCode(employee.getEmployeeCode())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .designationTitle(employee.getDesignation() != null ? employee.getDesignation().getName() : null)
                .status(saved.getStatus())
                .recordId(saved.getId())
                .build();
    }

    private Employee findEmployeeOrThrow(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }
}
