package com.hrms.bot.service.impl;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.attendance.entity.AttendanceMonthlySummary;
import com.hrms.attendance.repository.AttendanceMonthlySummaryRepository;
import com.hrms.bot.dto.BotChatResponse;
import com.hrms.bot.service.BotService;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.enums.LeaveRequestStatus;
import com.hrms.leave.repository.LeaveBalanceRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.payroll.repository.PayslipRepository;
import com.hrms.payroll.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BotServiceImpl implements BotService {

    private final UserRepository                     userRepository;
    private final EmployeeRepository                 employeeRepository;
    private final PayslipRepository                  payslipRepository;
    private final LeaveBalanceRepository             leaveBalanceRepository;
    private final LeaveRequestRepository             leaveRequestRepository;
    private final AttendanceMonthlySummaryRepository attendanceSummaryRepository;
    private final SalaryStructureRepository          salaryStructureRepository;

    private static final String[] MONTHS = {
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    @Override
    public BotChatResponse chat(String message, Authentication authentication) {
        String msg = message.toLowerCase().trim();
        boolean isHrAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN"));

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        UUID employeeId = user != null ? user.getEmployeeId() : null;

        // ── Greeting ──────────────────────────────────────────────────────────
        if (has(msg, "hi", "hello", "hey", "help", "good morning", "good afternoon",
                "good evening", "what can you do", "what can you help", "hi there")) {
            return reply(buildGreeting(isHrAdmin));
        }

        // ── Policy (check before leave keywords) ──────────────────────────────
        if (has(msg, "policy", "wfh", "work from home", "notice period",
                "office hour", "working hour", "probation", "holiday list")) {
            return reply(buildPolicyReply(msg));
        }

        // ── HR Admin intents ──────────────────────────────────────────────────
        if (isHrAdmin) {
            if (has(msg, "headcount", "how many employee", "total employee",
                    "active employee", "employee count", "new joiner", "joined this month")) {
                return reply(buildHeadcount());
            }
            if (has(msg, "payroll summary", "total salary", "total payroll",
                    "monthly payroll", "salary summary", "total monthly payroll")) {
                return reply(buildPayrollSummary());
            }
            if (has(msg, "pending leave", "leave approval", "pending approval",
                    "leaves to approve", "leave pending", "approve leave")) {
                return reply(buildHrPendingLeaves());
            }
            if (has(msg, "absent today", "who is absent", "today attendance",
                    "attendance today", "attendance overview", "attendance report", "staff attendance")) {
                return reply(buildHrAttendanceOverview());
            }
            if (has(msg, "find employee", "search employee", "employee detail",
                    "show employee", "look up employee", "employee info")) {
                return reply("To look up a specific employee, visit the Employees page where you can search by name, code, or department.");
            }
        }

        // ── Common intents (all roles, own data) ──────────────────────────────
        if (has(msg, "salary", "payslip", "net pay", "gross pay", "ctc",
                "my pay", "earning", "pay slip", "my salary", "deduction")) {
            return reply(buildSalaryReply(employeeId));
        }
        if (has(msg, "leave balance", "how many leave", "leave remaining",
                "available leave", "my leave balance", "leaves left")) {
            return reply(buildLeaveBalance(employeeId));
        }
        if (has(msg, "leave status", "my leave", "leave request",
                "applied leave", "leave history", "my leave request")) {
            return reply(buildLeaveStatus(employeeId));
        }
        if (has(msg, "attendance", "present day", "absent day", "lop day",
                "working day", "how many days", "my attendance", "how many present")) {
            return reply(buildAttendance(employeeId));
        }
        if (has(msg, "my profile", "my detail", "employee code", "my department",
                "my manager", "who am i", "my designation", "my info", "about me", "my name")) {
            return reply(buildProfile(employeeId));
        }

        return reply("I'm not sure I understand that. Try asking:\n" +
                     "• \"What is my salary?\"\n" +
                     "• \"What is my leave balance?\"\n" +
                     "• \"Show my attendance this month\"\n" +
                     "• \"What is my profile?\"\n" +
                     "• \"What is the WFH policy?\"\n\n" +
                     "Type \"help\" to see all options.");
    }

    // ── Greeting ──────────────────────────────────────────────────────────────

    private String buildGreeting(boolean isHrAdmin) {
        if (isHrAdmin) {
            return "Hello! I'm your HRMS Assistant.\n\n" +
                   "HR Admin options:\n" +
                   "• Employee headcount & new joiners\n" +
                   "• Monthly payroll summary\n" +
                   "• Pending leave approvals\n" +
                   "• Attendance overview\n\n" +
                   "Your personal info:\n" +
                   "• Your salary / payslip\n" +
                   "• Your leave balance\n" +
                   "• Your attendance\n" +
                   "• Your profile\n" +
                   "• Company policies\n\n" +
                   "What would you like to know?";
        }
        return "Hello! I'm your HRMS Assistant.\n\n" +
               "Here's what I can help you with:\n" +
               "• Your salary & payslip\n" +
               "• Leave balance & leave status\n" +
               "• Attendance summary\n" +
               "• Your profile details\n" +
               "• Company policies\n\n" +
               "What would you like to know?";
    }

    // ── Policy ────────────────────────────────────────────────────────────────

    private String buildPolicyReply(String msg) {
        if (msg.contains("wfh") || msg.contains("work from home")) {
            return "Work From Home Policy:\n\n" +
                   "Employees may work from home subject to manager approval and business requirements. " +
                   "Please coordinate with your reporting manager for WFH scheduling.";
        }
        if (msg.contains("notice")) {
            return "Notice Period Policy:\n\n" +
                   "• Junior roles: 30 days\n" +
                   "• Mid-level roles: 60 days\n" +
                   "• Senior roles: 90 days\n\n" +
                   "The exact notice period is as per your employment contract.";
        }
        if (msg.contains("probation")) {
            return "Probation Policy:\n\n" +
                   "New joiners serve a probation period of 3 to 6 months depending on the role. " +
                   "Performance is reviewed at the end of probation for confirmation.";
        }
        if (msg.contains("office hour") || msg.contains("working hour")) {
            return "Working Hours:\n\n" +
                   "• Standard hours: 9:00 AM – 6:00 PM\n" +
                   "• Monday to Friday\n" +
                   "• 1 hour lunch break included\n\n" +
                   "Flexible timing may apply based on department policy.";
        }
        if (msg.contains("holiday")) {
            return "Holiday Policy:\n\n" +
                   "Public holidays are observed as per the government holiday calendar. " +
                   "A company-specific holiday list is shared at the start of each year. " +
                   "Please check with HR for the current year's holiday list.";
        }
        return "Company Policies:\n\n" +
               "Ask me about specific policies:\n" +
               "• \"WFH policy\"\n" +
               "• \"Notice period policy\"\n" +
               "• \"Office hours\"\n" +
               "• \"Holiday policy\"\n" +
               "• \"Probation policy\"";
    }

    // ── Salary / Payslip ──────────────────────────────────────────────────────

    private String buildSalaryReply(UUID employeeId) {
        if (employeeId == null) return noEmployeeMsg();

        var page = payslipRepository.findLatestVisibleForEmployee(employeeId, PageRequest.of(0, 1));
        if (!page.isEmpty()) {
            var p = page.getContent().get(0);
            return "Your payslip for " + MONTHS[p.getMonth()] + " " + p.getYear() + ":\n\n" +
                   "Earnings:\n" +
                   "• Basic:        " + inr(p.getBasic()) + "\n" +
                   "• HRA:          " + inr(p.getHra()) + "\n" +
                   "• DA:           " + inr(p.getDa()) + "\n" +
                   "• Conveyance:   " + inr(p.getConveyance()) + "\n" +
                   "• Gross Pay:    " + inr(p.getGrossSalary()) + "\n\n" +
                   "Deductions:\n" +
                   "• TDS:          " + inr(p.getTds()) + "\n" +
                   "• PF:           " + inr(p.getPfDeduction()) + "\n" +
                   "• ESI:          " + inr(p.getEsiDeduction()) + "\n" +
                   "• Prof. Tax:    " + inr(p.getProfessionalTax()) + "\n\n" +
                   "Net Pay: " + inr(p.getNetSalary());
        }

        // Fallback: show salary structure if no payslip yet
        var structures = salaryStructureRepository.findActiveForEmployee(employeeId, LocalDate.now());
        if (!structures.isEmpty()) {
            var s = structures.get(0);
            return "No payslip generated yet.\n\n" +
                   "Your current salary structure:\n" +
                   "• Annual CTC:      " + inr(s.getAnnualCtc()) + "\n" +
                   "• Monthly Gross:   " + inr(s.getGrossSalary()) + "\n" +
                   "• Basic:           " + inr(s.getBasic()) + "\n" +
                   "• HRA:             " + inr(s.getHra()) + "\n" +
                   "• PF (Employee):   " + inr(s.getPfEmployee()) + "\n" +
                   "• Est. Net Salary: " + inr(s.getNetSalary()) + "\n\n" +
                   "Your payslip will appear here once HR generates it.";
        }

        return "No salary information found yet. Please contact HR if you believe this is incorrect.";
    }

    // ── Leave Balance ─────────────────────────────────────────────────────────

    private String buildLeaveBalance(UUID employeeId) {
        if (employeeId == null) return noEmployeeMsg();
        int year = LocalDate.now().getYear();
        var balances = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year);
        if (balances.isEmpty()) {
            return "No leave balance found for " + year + ".\nPlease contact HR to set up your leave allocation.";
        }
        StringBuilder sb = new StringBuilder("Your Leave Balance for " + year + ":\n\n");
        for (var b : balances) {
            BigDecimal remaining = b.getAllocatedDays()
                    .add(b.getCarriedForwardDays())
                    .subtract(b.getUsedDays())
                    .subtract(b.getPendingDays());
            sb.append("• ").append(b.getLeaveType().getName()).append(":\n")
              .append("  Remaining: ").append(fmt(remaining)).append(" days\n")
              .append("  Used: ").append(fmt(b.getUsedDays()))
              .append("  |  Pending: ").append(fmt(b.getPendingDays()))
              .append("  |  Total: ").append(fmt(b.getAllocatedDays().add(b.getCarriedForwardDays())))
              .append("\n\n");
        }
        return sb.toString().trim();
    }

    // ── Leave Status ──────────────────────────────────────────────────────────

    private String buildLeaveStatus(UUID employeeId) {
        if (employeeId == null) return noEmployeeMsg();
        var requests = leaveRequestRepository
                .findByEmployeeId(employeeId, PageRequest.of(0, 5, Sort.by("appliedAt").descending()))
                .getContent();
        if (requests.isEmpty()) return "You have no leave requests on record.";

        StringBuilder sb = new StringBuilder("Your recent leave requests:\n\n");
        for (var r : requests) {
            sb.append("• ").append(r.getLeaveType().getName())
              .append("\n  ").append(r.getStartDate()).append(" → ").append(r.getEndDate())
              .append(" (").append(fmt(r.getTotalDays())).append(" days)")
              .append("\n  Status: ").append(r.getStatus().name())
              .append("\n\n");
        }
        return sb.toString().trim();
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    private String buildAttendance(UUID employeeId) {
        if (employeeId == null) return noEmployeeMsg();
        var now = LocalDate.now();
        var opt = attendanceSummaryRepository.findByEmployeeIdAndYearAndMonth(
                employeeId, now.getYear(), now.getMonthValue());
        if (opt.isEmpty()) {
            return "No attendance record found for " + MONTHS[now.getMonthValue()] + " " + now.getYear() +
                   ".\nAttendance is typically updated after the month closes.";
        }
        var s = opt.get();
        return "Your attendance for " + MONTHS[s.getMonth()] + " " + s.getYear() + ":\n\n" +
               "• Working Days:  " + s.getWorkingDays() + "\n" +
               "• Present Days:  " + s.getPresentDays() + "\n" +
               "• Absent Days:   " + s.getAbsentDays() + "\n" +
               "• Late Days:     " + s.getLateDays() + "\n" +
               "• Half Days:     " + s.getHalfDays();
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private String buildProfile(UUID employeeId) {
        if (employeeId == null) return noEmployeeMsg();
        var opt = employeeRepository.findById(employeeId);
        if (opt.isEmpty()) return "Employee profile not found.";
        var e = opt.get();
        StringBuilder sb = new StringBuilder("Your Profile:\n\n");
        sb.append("• Name:         ").append(e.getFirstName()).append(" ").append(e.getLastName()).append("\n");
        sb.append("• Code:         ").append(e.getEmployeeCode()).append("\n");
        sb.append("• Email:        ").append(e.getEmail()).append("\n");
        if (e.getDepartment() != null)
            sb.append("• Department:   ").append(e.getDepartment().getName()).append("\n");
        if (e.getDesignation() != null)
            sb.append("• Designation:  ").append(e.getDesignation().getName()).append("\n");
        sb.append("• Joining Date: ").append(e.getJoiningDate()).append("\n");
        sb.append("• Status:       ").append(e.getEmploymentStatus().name()).append("\n");
        if (e.getManager() != null)
            sb.append("• Manager:      ").append(e.getManager().getFirstName()).append(" ").append(e.getManager().getLastName());
        return sb.toString().trim();
    }

    // ── HR: Headcount ─────────────────────────────────────────────────────────

    private String buildHeadcount() {
        long total = employeeRepository.count();
        long active = employeeRepository
                .findByEmploymentStatusOrderByFirstNameAscLastNameAsc(EmploymentStatus.ACTIVE).size();
        var now = LocalDate.now();
        var start = now.withDayOfMonth(1);
        var end = now.with(TemporalAdjusters.lastDayOfMonth());
        long joinedThisMonth = employeeRepository.findAll().stream()
                .filter(e -> e.getJoiningDate() != null
                        && !e.getJoiningDate().isBefore(start)
                        && !e.getJoiningDate().isAfter(end))
                .count();
        return "Employee Headcount:\n\n" +
               "• Total Employees:        " + total + "\n" +
               "• Active Employees:       " + active + "\n" +
               "• Inactive / Terminated:  " + (total - active) + "\n" +
               "• New Joiners (" + MONTHS[now.getMonthValue()] + "): " + joinedThisMonth;
    }

    // ── HR: Payroll Summary ───────────────────────────────────────────────────

    private String buildPayrollSummary() {
        var structures = salaryStructureRepository.findActiveForEmployeesByStatus(EmploymentStatus.ACTIVE);
        BigDecimal totalMonthly = structures.stream()
                .map(s -> s.getGrossSalary() != null ? s.getGrossSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAnnual = structures.stream()
                .map(s -> s.getAnnualCtc() != null ? s.getAnnualCtc() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long noSalary = employeeRepository
                .findByEmploymentStatusOrderByFirstNameAscLastNameAsc(EmploymentStatus.ACTIVE)
                .size() - structures.size();
        return "Monthly Payroll Summary:\n\n" +
               "• Employees with Salary Structure: " + structures.size() + "\n" +
               "• Employees without CTC assigned:  " + (noSalary < 0 ? 0 : noSalary) + "\n" +
               "• Total Monthly Payroll:           " + inr(totalMonthly) + "\n" +
               "• Total Annual CTC:                " + inr(totalAnnual);
    }

    // ── HR: Pending Leaves ────────────────────────────────────────────────────

    private String buildHrPendingLeaves() {
        long pending = leaveRequestRepository
                .findByStatus(LeaveRequestStatus.PENDING, PageRequest.of(0, 1))
                .getTotalElements();
        if (pending == 0)
            return "Great news! There are no pending leave requests at the moment.";
        return "Pending Leave Approvals:\n\n" +
               "• " + pending + " leave request" + (pending == 1 ? "" : "s") + " awaiting approval.\n\n" +
               "Visit the Leave Approvals page to review and action them.";
    }

    // ── HR: Attendance Overview ───────────────────────────────────────────────

    private String buildHrAttendanceOverview() {
        var now = LocalDate.now();
        var summaries = attendanceSummaryRepository.findByYearAndMonth(now.getYear(), now.getMonthValue());
        if (summaries.isEmpty()) {
            return "No attendance records found for " + MONTHS[now.getMonthValue()] + " " + now.getYear() +
                   ".\nAttendance data may not have been recorded yet this month.";
        }
        long totalPresent = summaries.stream().mapToLong(AttendanceMonthlySummary::getPresentDays).sum();
        long totalAbsent  = summaries.stream().mapToLong(AttendanceMonthlySummary::getAbsentDays).sum();
        long withAbsences = summaries.stream().filter(s -> s.getAbsentDays() > 0).count();
        return "Attendance Overview — " + MONTHS[now.getMonthValue()] + " " + now.getYear() + ":\n\n" +
               "• Employees Tracked:       " + summaries.size() + "\n" +
               "• Total Present Days:      " + totalPresent + "\n" +
               "• Total Absent Days:       " + totalAbsent + "\n" +
               "• Employees with Absences: " + withAbsences;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean has(String msg, String... keywords) {
        for (String kw : keywords) {
            if (msg.contains(kw)) return true;
        }
        return false;
    }

    private BotChatResponse reply(String text) {
        return new BotChatResponse(text);
    }

    private String noEmployeeMsg() {
        return "I couldn't find your employee profile linked to your account.\nPlease contact HR to resolve this.";
    }

    private String inr(BigDecimal amount) {
        if (amount == null) return "₹0";
        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        fmt.setMaximumFractionDigits(0);
        return "₹" + fmt.format(amount.longValue());
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0";
        return val.stripTrailingZeros().toPlainString();
    }
}
