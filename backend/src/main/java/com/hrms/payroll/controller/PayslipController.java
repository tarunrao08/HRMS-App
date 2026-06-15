package com.hrms.payroll.controller;

import com.hrms.auth.entity.User;
import com.hrms.auth.repository.UserRepository;
import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.payroll.dto.GeneratePayslipRequest;
import com.hrms.payroll.dto.PayslipResponse;
import com.hrms.payroll.entity.Payslip;
import com.hrms.payroll.repository.PayslipRepository;
import com.hrms.payroll.service.PayrollRunService;
import com.hrms.payroll.service.PayslipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/payroll/payslips")
@RequiredArgsConstructor
@Tag(name = "Payslips", description = "Access and manage employee payslips")
public class PayslipController {

    private final PayslipService    payslipService;
    private final PayrollRunService payrollRunService;
    private final PayslipRepository payslipRepository;
    private final UserRepository    userRepository;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    @Operation(summary = "Generate (or re-generate) a payslip for a specific employee and period")
    public ResponseEntity<ApiResponse<PayslipResponse>> generate(
            @Valid @RequestBody GeneratePayslipRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID requestedBy = userRepository.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(null);
        PayslipResponse response = payrollRunService.generatePayslipForEmployee(
                request.getEmployeeId(), request.getMonth(), request.getYear(), requestedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payslip generated successfully", response));
    }

    @GetMapping("/my/latest")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get the current user's latest published payslip")
    public ResponseEntity<ApiResponse<PayslipResponse>> getMyLatest(Authentication authentication) {
        UUID employeeId = resolveEmployeeId(authentication);
        if (employeeId == null) return ResponseEntity.notFound().build();
        return payslipService.getLatestPublished(employeeId)
                .map(p -> ResponseEntity.ok(ApiResponse.success(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my/all")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get the current user's full payslip history (paginated)")
    public ResponseEntity<ApiResponse<PageableResponse<PayslipResponse>>> getMyPayslips(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID employeeId = resolveEmployeeId(authentication);
        if (employeeId == null) return ResponseEntity.notFound().build();
        Pageable pageable = PageRequest.of(page, size, Sort.by("year", "month").descending());
        return ResponseEntity.ok(ApiResponse.paginated(payslipService.getMyPayslips(employeeId, pageable)));
    }

    @GetMapping("/employee/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get a payslip for an employee by year and month")
    public ResponseEntity<ApiResponse<PayslipResponse>> getByPeriod(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(payslipService.getByEmployeeAndPeriod(id, year, month)));
    }

    @GetMapping("/employee/{id}/all")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Get paginated payslip history for an employee (HR admin only)")
    public ResponseEntity<ApiResponse<PageableResponse<PayslipResponse>>> getByEmployee(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.paginated(payslipService.getByEmployee(id, pageable)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Publish a payslip to make it visible to the employee")
    public ResponseEntity<ApiResponse<PayslipResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Payslip published successfully", payslipService.publish(id)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER','EMPLOYEE')")
    @Operation(summary = "Download the generated PDF for a payslip")
    public ResponseEntity<Resource> downloadPdf(@PathVariable UUID id, Authentication authentication) {
        Payslip payslip = payslipRepository.findByIdWithEmployee(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", id.toString()));

        // Non-admin users may only download their own payslips
        boolean isPrivileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_ADMIN")
                            || a.getAuthority().equals("ROLE_MANAGER"));
        if (!isPrivileged) {
            UUID ownEmployeeId = resolveEmployeeId(authentication);
            if (ownEmployeeId == null || !payslip.getEmployee().getId().equals(ownEmployeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        if (payslip.getPdfUrl() == null) return ResponseEntity.notFound().build();
        File file = new File(payslip.getPdfUrl());
        if (!file.exists()) return ResponseEntity.notFound().build();

        String filename = String.format("payslip_%d_%02d_%s.pdf",
                payslip.getYear(), payslip.getMonth(),
                payslip.getEmployee().getEmployeeCode());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new FileSystemResource(file));
    }

    private UUID resolveEmployeeId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(User::getEmployeeId)
                .orElse(null);
    }
}
