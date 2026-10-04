package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.employee.dto.NomineeRequest;
import com.hrms.employee.dto.NomineeResponse;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.entity.EmployeeNominee;
import com.hrms.employee.mapper.NomineeMapper;
import com.hrms.employee.repository.EmployeeNomineeRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.service.EmployeeNomineeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeNomineeServiceImpl implements EmployeeNomineeService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EmployeeNomineeRepository nomineeRepository;
    private final EmployeeRepository        employeeRepository;
    private final NomineeMapper             nomineeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<NomineeResponse> list(UUID employeeId) {
        requireEmployee(employeeId);
        return nomineeMapper.toResponseList(nomineeRepository.findByEmployeeId(employeeId));
    }

    @Override
    @Transactional
    public NomineeResponse add(UUID employeeId, NomineeRequest request) {
        Employee employee = requireEmployee(employeeId);
        List<EmployeeNominee> existing = nomineeRepository.findByEmployeeId(employeeId);
        validateShareNotExceeded(existing, null, request.getSharePercentage());
        normalizeAndValidateMinor(request);

        EmployeeNominee nominee = nomineeMapper.toEntity(request);
        nominee.setEmployee(employee);
        nominee = nomineeRepository.save(nominee);
        return nomineeMapper.toResponse(nominee);
    }

    @Override
    @Transactional
    public NomineeResponse update(UUID employeeId, UUID nomineeId, NomineeRequest request) {
        requireEmployee(employeeId);
        EmployeeNominee nominee = nomineeRepository.findByIdAndEmployeeId(nomineeId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nominee", "id", nomineeId.toString()));

        List<EmployeeNominee> existing = nomineeRepository.findByEmployeeId(employeeId);
        validateShareNotExceeded(existing, nomineeId, request.getSharePercentage());
        normalizeAndValidateMinor(request);

        nominee.setName(request.getName());
        nominee.setRelationship(request.getRelationship());
        nominee.setDateOfBirth(request.getDateOfBirth());
        nominee.setSharePercentage(request.getSharePercentage());
        nominee.setAddress(request.getAddress());
        nominee.setContactNumber(request.getContactNumber());
        nominee.setMinor(request.isMinor());
        nominee.setGuardianName(request.getGuardianName());
        nominee.setGuardianRelationship(request.getGuardianRelationship());
        nominee = nomineeRepository.save(nominee);
        return nomineeMapper.toResponse(nominee);
    }

    @Override
    @Transactional
    public void delete(UUID employeeId, UUID nomineeId) {
        requireEmployee(employeeId);
        EmployeeNominee nominee = nomineeRepository.findByIdAndEmployeeId(nomineeId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nominee", "id", nomineeId.toString()));
        nomineeRepository.delete(nominee);
    }

    @Override
    @Transactional
    public List<NomineeResponse> replaceAll(UUID employeeId, List<NomineeRequest> requests) {
        Employee employee = requireEmployee(employeeId);

        if (requests != null && !requests.isEmpty()) {
            requests.forEach(this::normalizeAndValidateMinor);

            // Nominees are not required to collectively add up to exactly 100 — each nominee's own
            // share is already bounded to 0–100 by NomineeRequest — but the total still can't exceed
            // 100, same rule the single add()/update() endpoints enforce via validateShareNotExceeded.
            BigDecimal total = requests.stream()
                    .map(NomineeRequest::getSharePercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(HUNDRED) > 0) {
                throw new ValidationException("Total nominee share percentage cannot exceed 100, got " + total);
            }
        }

        nomineeRepository.deleteByEmployeeId(employeeId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<EmployeeNominee> nominees = requests.stream()
                .map(request -> {
                    EmployeeNominee nominee = nomineeMapper.toEntity(request);
                    nominee.setEmployee(employee);
                    return nominee;
                })
                .toList();
        return nomineeMapper.toResponseList(nomineeRepository.saveAll(nominees));
    }

    /**
     * The minor flag must never be taken on the client's word — it's a legal fact derived from
     * date of birth, so it's recomputed here from {@code dateOfBirth} on every write, overwriting
     * whatever the caller sent. When that computation says the nominee is a minor, guardian details
     * become mandatory, matching the statutory requirement for minor nominees (e.g. EPF/insurance forms).
     */
    private void normalizeAndValidateMinor(NomineeRequest request) {
        boolean minor = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears() < 18;
        request.setMinor(minor);
        if (minor) {
            if (request.getGuardianName() == null || request.getGuardianName().isBlank()) {
                throw new ValidationException("Guardian name is required for a minor nominee");
            }
            if (request.getGuardianRelationship() == null) {
                throw new ValidationException("Guardian relationship is required for a minor nominee");
            }
        }
    }

    private void validateShareNotExceeded(List<EmployeeNominee> existing, UUID excludeId, BigDecimal incomingShare) {
        BigDecimal total = existing.stream()
                .filter(n -> excludeId == null || !n.getId().equals(excludeId))
                .map(EmployeeNominee::getSharePercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(incomingShare);
        if (total.compareTo(HUNDRED) > 0) {
            throw new ValidationException("Total nominee share percentage cannot exceed 100, would be " + total);
        }
    }

    private Employee requireEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }
}
