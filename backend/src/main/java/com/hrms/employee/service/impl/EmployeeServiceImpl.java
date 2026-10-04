package com.hrms.employee.service.impl;

import com.hrms.attendance.entity.EmployeeShift;
import com.hrms.attendance.entity.Shift;
import com.hrms.attendance.repository.EmployeeShiftRepository;
import com.hrms.attendance.repository.ShiftRepository;
import com.hrms.common.dto.PageableResponse;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.EmployeeNameResponse;
import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.EmployeeSummaryResponse;
import com.hrms.employee.entity.Branch;
import com.hrms.employee.entity.Department;
import com.hrms.employee.entity.Designation;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.enums.EmploymentStatus;
import com.hrms.employee.enums.EmploymentType;
import com.hrms.employee.mapper.EmployeeMapper;
import com.hrms.employee.mapper.FamilyMemberMapper;
import com.hrms.employee.mapper.NomineeMapper;
import com.hrms.employee.repository.BranchRepository;
import com.hrms.employee.repository.DepartmentRepository;
import com.hrms.employee.repository.DesignationRepository;
import com.hrms.employee.repository.EmployeeFamilyMemberRepository;
import com.hrms.employee.repository.EmployeeNomineeRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSpecification;
import com.hrms.employee.event.EmployeeCreatedEvent;
import com.hrms.employee.service.EmployeeFamilyMemberService;
import com.hrms.employee.service.EmployeeNomineeService;
import com.hrms.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository       employeeRepository;
    private final DepartmentRepository     departmentRepository;
    private final DesignationRepository    designationRepository;
    private final BranchRepository         branchRepository;
    private final EmployeeShiftRepository  employeeShiftRepository;
    private final ShiftRepository          shiftRepository;
    private final EmployeeMapper           employeeMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final EmployeeFamilyMemberService  familyMemberService;
    private final EmployeeNomineeService       nomineeService;
    private final EmployeeFamilyMemberRepository familyMemberRepository;
    private final EmployeeNomineeRepository    nomineeRepository;
    private final FamilyMemberMapper           familyMemberMapper;
    private final NomineeMapper                nomineeMapper;

    private static final int MINIMUM_AGE = 21;

    private void validateAge(LocalDate dateOfBirth) {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now().minusYears(MINIMUM_AGE))) {
            throw new ValidationException("Employee must be at least " + MINIMUM_AGE + " years old");
        }
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("An employee with email '" + request.getEmail() + "' already exists");
        }
        validateAge(request.getDateOfBirth());

        Employee employee = employeeMapper.toEntity(request);
        employee.setEmployeeCode(generateEmployeeCode());

        resolveRelations(employee, request);

        if (employee.getEmploymentStatus() == null) {
            employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }
        if (employee.getEmploymentType() == null) {
            employee.setEmploymentType(EmploymentType.FULL_TIME);
        }

        employee = employeeRepository.save(employee);
        log.info("Created employee: {} ({})", employee.getEmployeeCode(), employee.getEmail());

        if (request.getShiftId() != null) {
            assignShift(employee, request.getShiftId(), request.getJoiningDate());
        }

        if (request.getFamilyMembers() != null) {
            familyMemberService.replaceAll(employee.getId(), request.getFamilyMembers());
        }
        if (request.getNominees() != null) {
            nomineeService.replaceAll(employee.getId(), request.getNominees());
        }

        eventPublisher.publishEvent(new EmployeeCreatedEvent(this, employee.getId()));
        return withFamilyAndNominees(employeeMapper.toResponse(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return withFamilyAndNominees(employeeMapper.toResponse(findOrThrow(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getByCode(String code) {
        Employee employee = employeeRepository.findByEmployeeCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "code", code));
        return withFamilyAndNominees(employeeMapper.toResponse(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public PageableResponse<EmployeeSummaryResponse> search(String search, UUID departmentId, UUID designationId,
                                                             UUID branchId, UUID managerId,
                                                             EmploymentStatus status, EmploymentType type,
                                                             Sort.Direction nameSortDirection,
                                                             Pageable pageable) {
        Specification<Employee> spec = EmployeeSpecification.filter(
                search, departmentId, designationId, branchId, managerId, status, type, nameSortDirection);
        Page<EmployeeSummaryResponse> page = employeeRepository.findAll(spec, pageable)
                .map(employeeMapper::toSummary);
        return PageableResponse.of(page);
    }

    @Override
    @Transactional
    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        Employee employee = findOrThrow(id);

        if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("An employee with email '" + request.getEmail() + "' already exists");
        }
        validateAge(request.getDateOfBirth());

        employeeMapper.updateEntity(employee, request);
        resolveRelations(employee, request);

        employee = employeeRepository.save(employee);
        log.info("Updated employee: {}", employee.getEmployeeCode());

        updateShiftAssignment(employee, request.getShiftId());

        if (request.getFamilyMembers() != null) {
            familyMemberService.replaceAll(employee.getId(), request.getFamilyMembers());
        }
        if (request.getNominees() != null) {
            nomineeService.replaceAll(employee.getId(), request.getNominees());
        }

        return withFamilyAndNominees(employeeMapper.toResponse(employee));
    }

    private EmployeeResponse withFamilyAndNominees(EmployeeResponse response) {
        response.setFamilyMembers(familyMemberMapper.toResponseList(
                familyMemberRepository.findByEmployeeId(response.getId())));
        response.setNominees(nomineeMapper.toResponseList(
                nomineeRepository.findByEmployeeId(response.getId())));
        return response;
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Employee employee = findOrThrow(id);
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        employeeRepository.save(employee);
        log.info("Deactivated employee: {}", employee.getEmployeeCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> getDirectReports(UUID managerId) {
        findOrThrow(managerId);
        return employeeRepository.findByManagerId(managerId).stream()
                .map(employeeMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeNameResponse> getAllSummaries() {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getEmploymentStatus() != EmploymentStatus.INACTIVE)
                .map(e -> new EmployeeNameResponse(e.getId(), e.getEmployeeCode(),
                        e.getFirstName() + " " + e.getLastName(),
                        e.getDepartment() != null ? e.getDepartment().getId() : null,
                        e.getDesignation() != null && e.getDesignation().isManagerial()))
                .toList();
    }

    private Employee findOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id.toString()));
    }

    private void resolveRelations(Employee employee, EmployeeRequest request) {
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId().toString()));
            employee.setDepartment(dept);
        } else {
            employee.setDepartment(null);
        }

        if (request.getDesignationId() != null) {
            Designation desig = designationRepository.findById(request.getDesignationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", request.getDesignationId().toString()));
            employee.setDesignation(desig);
        } else {
            employee.setDesignation(null);
        }

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Branch", "id", request.getBranchId().toString()));
            employee.setBranch(branch);
        } else {
            employee.setBranch(null);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee (manager)", "id", request.getManagerId().toString()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        // A department must actually be offered by the chosen branch — the create/edit UI only
        // lets HR pick from that branch's departments, but this is the server-side backstop.
        if (employee.getDepartment() != null && employee.getBranch() != null) {
            Department department = employee.getDepartment();
            Branch branch = employee.getBranch();
            boolean offered = branch.getDepartments().stream().anyMatch(d -> d.getId().equals(department.getId()));
            if (!offered) {
                throw new ValidationException("Department '" + department.getName() + "' is not offered by branch '"
                        + branch.getName() + "'");
            }
        }
    }

    private void assignShift(Employee employee, UUID shiftId, LocalDate effectiveFrom) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", shiftId.toString()));
        EmployeeShift employeeShift = EmployeeShift.builder()
                .employee(employee)
                .shift(shift)
                .effectiveFrom(effectiveFrom)
                .build();
        employeeShiftRepository.save(employeeShift);
    }

    private void updateShiftAssignment(Employee employee, UUID newShiftId) {
        LocalDate today = LocalDate.now();
        List<EmployeeShift> active = employeeShiftRepository.findActiveShiftForEmployee(employee.getId(), today);
        EmployeeShift current = active.isEmpty() ? null : active.get(0);
        UUID currentShiftId = current != null ? current.getShift().getId() : null;

        if (java.util.Objects.equals(currentShiftId, newShiftId)) {
            return;
        }
        if (current != null) {
            current.setEffectiveTo(today);
            employeeShiftRepository.save(current);
        }
        if (newShiftId != null) {
            assignShift(employee, newShiftId, today);
        }
    }

    private String generateEmployeeCode() {
        return employeeRepository.findMaxEmployeeCode()
                .map(last -> {
                    String numPart = last.substring(3);
                    int next = Integer.parseInt(numPart) + 1;
                    return String.format("EMP%05d", next);
                })
                .orElse("EMP00001");
    }
}
