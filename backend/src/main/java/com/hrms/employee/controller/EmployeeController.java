package com.hrms.employee.controller;

import com.hrms.common.dto.ApiResponse;
import com.hrms.common.dto.PageableResponse;
import com.hrms.employee.dto.EmployeeNameResponse;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.EmployeeSummaryResponse;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Manage employee records")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Onboard a new employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Search and filter employees with pagination")
    public ResponseEntity<ApiResponse<PageableResponse<EmployeeSummaryResponse>>> search(
            @Parameter(description = "Search by name, email, or employee code")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID designationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID managerId,
            @RequestParam(name = "employmentStatus", required = false) EmploymentStatus status,
            @RequestParam(name = "employmentType",   required = false) EmploymentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PageableResponse<EmployeeSummaryResponse> result = employeeService.search(
                search, departmentId, designationId, branchId, managerId, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.paginated(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full employee profile by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get full employee profile by employee code")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getByCode(code)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Update an employee record")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", employeeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Deactivate an employee (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        employeeService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully"));
    }

    @GetMapping("/summaries")
    @Operation(summary = "Get minimal employee list for dropdowns")
    public ResponseEntity<ApiResponse<List<EmployeeNameResponse>>> getSummaries() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getAllSummaries()));
    }

    @GetMapping("/{id}/reports")
    @Operation(summary = "Get direct reports of a manager")
    public ResponseEntity<ApiResponse<List<EmployeeSummaryResponse>>> getDirectReports(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getDirectReports(id)));
    }
}
