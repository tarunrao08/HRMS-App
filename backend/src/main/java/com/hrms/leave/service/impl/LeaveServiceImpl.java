package com.hrms.leave.service.impl;

import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.entity.*;
import com.hrms.leave.enums.LeaveApprovalAction;
import com.hrms.leave.enums.LeaveRequestStatus;
import com.hrms.leave.mapper.LeaveMapper;
import com.hrms.leave.repository.*;
import com.hrms.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository  leaveRequestRepository;
    private final LeaveBalanceRepository  leaveBalanceRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveTypeRepository     leaveTypeRepository;
    private final EmployeeRepository      employeeRepository;
    private final LeaveMapper             leaveMapper;

    @Override
    @Transactional
    public LeaveRequestResponse apply(LeaveRequestRequest req) {
        Employee  employee  = findEmployeeOrThrow(req.getEmployeeId());
        LeaveType leaveType = findLeaveTypeOrThrow(req.getLeaveTypeId());

        if (!leaveType.isActive()) {
            throw new ValidationException("Leave type '" + leaveType.getName() + "' is not active");
        }

        LocalDate today = LocalDate.now();
        if (leaveType.getMinNoticeDays() > 0 && req.getStartDate().isBefore(today.plusDays(leaveType.getMinNoticeDays()))) {
            throw new ValidationException(
                    "This leave type requires at least " + leaveType.getMinNoticeDays() + " notice day(s) before the start date");
        }

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ValidationException("End date must not be before start date");
        }

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlapping(
                req.getEmployeeId(), req.getStartDate(), req.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new ValidationException("Employee already has a pending or approved leave overlapping the requested period");
        }

        BigDecimal totalDays = computeTotalDays(req.getStartDate(), req.getEndDate(), req.isHalfDay());

        int year = req.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(req.getEmployeeId(), req.getLeaveTypeId(), year)
                .orElseThrow(() -> new ValidationException(
                        "No leave balance allocated for employee and leave type in year " + year));

        BigDecimal available = balance.getAllocatedDays()
                .add(balance.getCarriedForwardDays())
                .subtract(balance.getUsedDays())
                .subtract(balance.getPendingDays());

        if (available.compareTo(totalDays) < 0) {
            throw new ValidationException(
                    "Insufficient leave balance. Available: " + available + ", Requested: " + totalDays);
        }

        balance.setPendingDays(balance.getPendingDays().add(totalDays));
        leaveBalanceRepository.save(balance);

        Employee currentApprover = null;
        if (req.getCurrentApproverId() != null) {
            currentApprover = findEmployeeOrThrow(req.getCurrentApproverId());
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(leaveType)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .totalDays(totalDays)
                .halfDay(req.isHalfDay())
                .halfDayType(req.getHalfDayType())
                .reason(req.getReason())
                .documentUrl(req.getDocumentUrl())
                .currentApprover(currentApprover)
                .status(LeaveRequestStatus.PENDING)
                .appliedAt(Instant.now())
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request applied by employee {} for {} days of {}", employee.getId(), totalDays, leaveType.getCode());
        return leaveMapper.toRequestResponse(leaveRequest);
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(UUID requestId, UUID approverId, LeaveApprovalRequest req) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ValidationException("Only PENDING leave requests can be approved");
        }
        int year = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                .orElseThrow(() -> new ValidationException("Leave balance record not found"));
        balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getTotalDays()));
        leaveBalanceRepository.save(balance);
        leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
        final LeaveRequest savedApproved = leaveRequestRepository.save(leaveRequest);
        if (approverId != null) {
            employeeRepository.findById(approverId)
                    .ifPresent(approver -> saveApprovalRecord(savedApproved, approver, req, LeaveApprovalAction.APPROVED));
        }
        log.info("Leave request {} approved by {}", requestId, approverId);
        return leaveMapper.toRequestResponse(savedApproved);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(UUID requestId, UUID approverId, LeaveApprovalRequest req) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ValidationException("Only PENDING leave requests can be rejected");
        }
        int year = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                .orElseThrow(() -> new ValidationException("Leave balance record not found"));
        balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        leaveBalanceRepository.save(balance);
        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        leaveRequest.setRejectionReason(req != null ? req.getComments() : null);
        final LeaveRequest savedRejected = leaveRequestRepository.save(leaveRequest);
        if (approverId != null) {
            final LeaveApprovalRequest effectiveReq = req != null ? req : new LeaveApprovalRequest();
            employeeRepository.findById(approverId)
                    .ifPresent(approver -> saveApprovalRecord(savedRejected, approver, effectiveReq, LeaveApprovalAction.REJECTED));
        }
        log.info("Leave request {} rejected by {}", requestId, approverId);
        return leaveMapper.toRequestResponse(savedRejected);
    }

    @Override
    @Transactional
    public LeaveRequestResponse cancel(UUID requestId, UUID cancelledByUserId) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ValidationException("Only PENDING leave requests can be cancelled");
        }
        int year = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                .orElseThrow(() -> new ValidationException("Leave balance record not found"));
        balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        leaveBalanceRepository.save(balance);
        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequest.setCancelledAt(Instant.now());
        leaveRequest.setCancelledBy(cancelledByUserId);
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} cancelled by user {}", requestId, cancelledByUserId);
        return leaveMapper.toRequestResponse(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(UUID employeeId, int year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year).stream()
                .map(leaveMapper::toBalanceResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse allocateBalance(UUID employeeId, UUID leaveTypeId, int year, BigDecimal days) {
        findEmployeeOrThrow(employeeId);
        findLeaveTypeOrThrow(leaveTypeId);

        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, leaveTypeId, year)
                .orElseGet(() -> {
                    Employee  emp  = employeeRepository.getReferenceById(employeeId);
                    LeaveType type = leaveTypeRepository.getReferenceById(leaveTypeId);
                    return LeaveBalance.builder()
                            .employee(emp)
                            .leaveType(type)
                            .year(year)
                            .build();
                });

        balance.setAllocatedDays(days);
        balance = leaveBalanceRepository.save(balance);
        log.info("Allocated {} days of leave type {} to employee {} for year {}", days, leaveTypeId, employeeId, year);
        return leaveMapper.toBalanceResponse(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<LeaveRequestResponse> getRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable) {
        Page<LeaveRequest> page = status != null
                ? leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable)
                : leaveRequestRepository.findByEmployeeId(employeeId, pageable);
        return PageableResponse.of(page.map(leaveMapper::toRequestResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<LeaveRequestResponse> getAllRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable) {
        Page<LeaveRequest> page;
        if (employeeId != null && status != null) {
            page = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        } else if (employeeId != null) {
            page = leaveRequestRepository.findByEmployeeId(employeeId, pageable);
        } else if (status != null) {
            page = leaveRequestRepository.findByStatus(status, pageable);
        } else {
            page = leaveRequestRepository.findAll(pageable);
        }
        return PageableResponse.of(page.map(leaveMapper::toRequestResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingForApprover(UUID approverId) {
        return leaveRequestRepository
                .findByCurrentApproverIdAndStatus(approverId, LeaveRequestStatus.PENDING)
                .stream()
                .map(leaveMapper::toRequestResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveApprovalResponse> getApprovals(UUID requestId) {
        findRequestOrThrow(requestId);
        return leaveApprovalRepository.findByLeaveRequestIdOrderByApproverLevelAsc(requestId)
                .stream()
                .map(leaveMapper::toApprovalResponse)
                .toList();
    }

    private void saveApprovalRecord(LeaveRequest leaveRequest, Employee approver,
                                    LeaveApprovalRequest req, LeaveApprovalAction action) {
        int nextLevel = leaveApprovalRepository
                .findByLeaveRequestIdOrderByApproverLevelAsc(leaveRequest.getId())
                .size() + 1;

        LeaveApproval approval = LeaveApproval.builder()
                .leaveRequest(leaveRequest)
                .approver(approver)
                .approverLevel(nextLevel)
                .action(action)
                .comments(req.getComments())
                .actedAt(Instant.now())
                .build();
        leaveApprovalRepository.save(approval);
    }

    private BigDecimal computeTotalDays(LocalDate startDate, LocalDate endDate, boolean halfDay) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (halfDay) {
            return BigDecimal.valueOf(0.5);
        }
        return BigDecimal.valueOf(days);
    }

    private Employee findEmployeeOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id.toString()));
    }

    private LeaveType findLeaveTypeOrThrow(UUID id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", id.toString()));
    }

    private LeaveRequest findRequestOrThrow(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id.toString()));
    }
}
