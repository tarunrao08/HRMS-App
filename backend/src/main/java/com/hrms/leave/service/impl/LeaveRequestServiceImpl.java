package com.hrms.leave.service.impl;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.AppException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.dto.*;
import com.hrms.leave.entity.*;
import com.hrms.leave.enums.LeaveApprovalAction;
import com.hrms.leave.enums.LeaveApprovalStatus;
import com.hrms.leave.enums.LeaveRequestStatus;
import com.hrms.leave.event.LeaveApprovedEvent;
import com.hrms.leave.mapper.LeaveMapper;
import com.hrms.leave.repository.*;
import com.hrms.leave.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository  leaveRequestRepository;
    private final LeaveBalanceRepository  leaveBalanceRepository;
    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveTypeRepository     leaveTypeRepository;
    private final EmployeeRepository      employeeRepository;
    private final UserRepository          userRepository;
    private final LeaveMapper             leaveMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ── Item 6: Submit leave request ──────────────────────────────────────────

    @Override
    @Transactional
    public LeaveRequestResponse submit(UUID employeeId, ApplyLeaveRequest req) {
        Employee  employee  = findEmployeeOrThrow(employeeId);
        LeaveType leaveType = findLeaveTypeOrThrow(req.getLeaveTypeId());

        if (!leaveType.isActive()) {
            throw new ValidationException("Leave type '" + leaveType.getName() + "' is not active");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ValidationException("End date must not be before start date");
        }

        LocalDate today = LocalDate.now();
        if (leaveType.getMinNoticeDays() > 0
                && req.getStartDate().isBefore(today.plusDays(leaveType.getMinNoticeDays()))) {
            throw new ValidationException(
                    "This leave type requires at least " + leaveType.getMinNoticeDays() + " notice day(s)");
        }

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlapping(
                employeeId, req.getStartDate(), req.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new ValidationException(
                    "You already have a pending or approved leave overlapping the requested dates");
        }

        BigDecimal totalDays = countWorkdays(req.getStartDate(), req.getEndDate(), req.isHalfDay());
        if (totalDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("The selected date range contains no working days (Mon–Fri)");
        }

        int year = req.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employeeId, req.getLeaveTypeId(), year)
                .orElseThrow(() -> new ValidationException(
                        "No leave balance allocated for '" + leaveType.getName() + "' in " + year));

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
                .status(LeaveRequestStatus.PENDING)
                .currentApprovalLevel(1)
                .appliedAt(Instant.now())
                .build();
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        List<Employee> chain = buildApprovalChain(employee);
        for (int i = 0; i < chain.size(); i++) {
            LeaveApproval approval = LeaveApproval.builder()
                    .leaveRequest(leaveRequest)
                    .approver(chain.get(i))
                    .approverLevel(i + 1)
                    .status(LeaveApprovalStatus.PENDING)
                    .build();
            leaveApprovalRepository.save(approval);
        }

        log.info("Leave request {} submitted by employee {} for {} workdays of {}",
                leaveRequest.getId(), employeeId, totalDays, leaveType.getCode());
        return leaveMapper.toRequestResponse(leaveRequest);
    }

    // ── Item 7: Approval workflow ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingApprovals(UUID approverId) {
        return leaveApprovalRepository.findPendingLeaveRequestsForApprover(approverId)
                .stream()
                .map(leaveMapper::toRequestResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingApprovalsForHrAdmin() {
        return leaveApprovalRepository.findPendingLeaveRequestsForHrAdminRole()
                .stream()
                .map(leaveMapper::toRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(UUID requestId, UUID approverId, ApproveRejectRequest req) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ValidationException("Only PENDING leave requests can be approved");
        }

        int currentLevel = leaveRequest.getCurrentApprovalLevel();
        LeaveApproval approval = leaveApprovalRepository
                .findByLeaveRequestIdAndApproverLevel(requestId, currentLevel)
                .orElseThrow(() -> new AppException(
                        "No approval record found for level " + currentLevel,
                        HttpStatus.NOT_FOUND, "APPROVAL_NOT_FOUND"));

        // null approverId means HR Admin override — any HR Admin can approve any level
        if (approverId != null && !approval.getApprover().getId().equals(approverId)) {
            throw new AppException(
                    "You are not authorized to approve this request at the current level",
                    HttpStatus.FORBIDDEN, "NOT_AUTHORIZED_APPROVER");
        }
        if (approval.getStatus() != LeaveApprovalStatus.PENDING) {
            throw new ValidationException("This approval step has already been acted upon");
        }

        approval.setStatus(LeaveApprovalStatus.APPROVED);
        approval.setAction(LeaveApprovalAction.APPROVED);
        approval.setActedAt(Instant.now());
        approval.setComments(req != null ? req.getComments() : null);
        leaveApprovalRepository.save(approval);

        int nextLevel = leaveRequest.getCurrentApprovalLevel() + 1;
        boolean hasNextLevel = leaveApprovalRepository
                .findByLeaveRequestIdAndApproverLevel(requestId, nextLevel).isPresent();

        if (hasNextLevel) {
            leaveRequest.setCurrentApprovalLevel(nextLevel);
            leaveRequest = leaveRequestRepository.save(leaveRequest);
            log.info("Leave request {} forwarded to approval level {}", requestId, nextLevel);
        } else {
            int year = leaveRequest.getStartDate().getYear();
            LeaveBalance balance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(
                            leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                    .orElseThrow(() -> new ValidationException("Leave balance record not found"));
            balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
            balance.setUsedDays(balance.getUsedDays().add(leaveRequest.getTotalDays()));
            leaveBalanceRepository.save(balance);

            leaveRequest.setStatus(LeaveRequestStatus.APPROVED);
            leaveRequest = leaveRequestRepository.save(leaveRequest);

            eventPublisher.publishEvent(new LeaveApprovedEvent(
                    leaveRequest.getEmployee().getId(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate()));
            log.info("Leave request {} fully approved", requestId);
        }

        return leaveMapper.toRequestResponse(leaveRequest);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(UUID requestId, UUID approverId, ApproveRejectRequest req) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new ValidationException("Only PENDING leave requests can be rejected");
        }

        int currentLevel = leaveRequest.getCurrentApprovalLevel();
        LeaveApproval approval = leaveApprovalRepository
                .findByLeaveRequestIdAndApproverLevel(requestId, currentLevel)
                .orElseThrow(() -> new AppException(
                        "No approval record found for level " + currentLevel,
                        HttpStatus.NOT_FOUND, "APPROVAL_NOT_FOUND"));

        // null approverId means HR Admin override — any HR Admin can reject any level
        if (approverId != null && !approval.getApprover().getId().equals(approverId)) {
            throw new AppException(
                    "You are not authorized to reject this request at the current level",
                    HttpStatus.FORBIDDEN, "NOT_AUTHORIZED_APPROVER");
        }
        if (approval.getStatus() != LeaveApprovalStatus.PENDING) {
            throw new ValidationException("This approval step has already been acted upon");
        }

        approval.setStatus(LeaveApprovalStatus.REJECTED);
        approval.setAction(LeaveApprovalAction.REJECTED);
        approval.setActedAt(Instant.now());
        approval.setComments(req != null ? req.getComments() : null);
        leaveApprovalRepository.save(approval);

        int year = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                .orElseThrow(() -> new ValidationException("Leave balance record not found"));
        balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        leaveBalanceRepository.save(balance);

        leaveRequest.setStatus(LeaveRequestStatus.REJECTED);
        leaveRequest.setRejectionReason(req != null ? req.getComments() : null);
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} rejected at level {}", requestId, leaveRequest.getCurrentApprovalLevel());
        return leaveMapper.toRequestResponse(leaveRequest);
    }

    @Override
    @Transactional
    public LeaveRequestResponse cancel(UUID requestId, UUID cancelledByUserId) {
        LeaveRequest leaveRequest = findRequestOrThrow(requestId);

        boolean isPending  = leaveRequest.getStatus() == LeaveRequestStatus.PENDING;
        boolean isApprovedFuture = leaveRequest.getStatus() == LeaveRequestStatus.APPROVED
                && leaveRequest.getStartDate().isAfter(LocalDate.now());

        if (!isPending && !isApprovedFuture) {
            throw new ValidationException(
                    "Leave request can only be cancelled when PENDING or when APPROVED with a future start date");
        }

        int year = leaveRequest.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType().getId(), year)
                .orElseThrow(() -> new ValidationException("Leave balance record not found"));

        if (isPending) {
            balance.setPendingDays(balance.getPendingDays().subtract(leaveRequest.getTotalDays()));
        } else {
            balance.setUsedDays(balance.getUsedDays().subtract(leaveRequest.getTotalDays()));
        }
        leaveBalanceRepository.save(balance);

        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequest.setCancelledAt(Instant.now());
        leaveRequest.setCancelledBy(cancelledByUserId);
        leaveRequest = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request {} cancelled by user {}", requestId, cancelledByUserId);
        return leaveMapper.toRequestResponse(leaveRequest);
    }

    // ── Item 6 / 8: Paginated queries ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<LeaveRequestResponse> getMyRequests(UUID employeeId, LeaveRequestStatus status, Pageable pageable) {
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal countWorkdays(LocalDate start, LocalDate end, boolean halfDay) {
        if (halfDay) {
            return new BigDecimal("0.5");
        }
        long count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return BigDecimal.valueOf(count);
    }

    private List<Employee> buildApprovalChain(Employee employee) {
        Employee hrAdmin = resolveHrAdminEmployee();

        // Managers and HR Admins go directly to HR Admin for approval
        boolean isManagerOrAbove = userRepository.findByEmployeeId(employee.getId())
                .map(u -> u.getRoles().stream()
                        .anyMatch(r -> "ROLE_MANAGER".equals(r.getName())
                                || "ROLE_HR_ADMIN".equals(r.getName())))
                .orElse(false);

        if (isManagerOrAbove) {
            return List.of(hrAdmin);
        }

        // Regular employees: department manager (L1) → HR Admin (L2)
        Employee manager = employee.getManager();
        if (manager == null) {
            // No manager assigned — escalate directly to HR Admin
            return List.of(hrAdmin);
        }
        // Avoid duplicate if the employee's manager is the HR Admin
        if (manager.getId().equals(hrAdmin.getId())) {
            return List.of(hrAdmin);
        }
        return List.of(manager, hrAdmin);
    }

    private Employee resolveHrAdminEmployee() {
        List<User> hrAdmins = userRepository.findByRoleName("ROLE_HR_ADMIN");
        return hrAdmins.stream()
                .filter(u -> u.getEmployeeId() != null)
                .findFirst()
                .flatMap(u -> employeeRepository.findById(u.getEmployeeId()))
                .orElseThrow(() -> new AppException(
                        "No HR Admin employee found to serve as fallback approver",
                        HttpStatus.INTERNAL_SERVER_ERROR, "NO_HR_ADMIN_EMPLOYEE"));
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
