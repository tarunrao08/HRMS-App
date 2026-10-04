package com.hrms.employee.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.dto.FamilyMemberRequest;
import com.hrms.employee.dto.FamilyMemberResponse;
import com.hrms.employee.entity.Employee;
import com.hrms.employee.entity.EmployeeFamilyMember;
import com.hrms.employee.mapper.FamilyMemberMapper;
import com.hrms.employee.repository.EmployeeFamilyMemberRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.service.EmployeeFamilyMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeFamilyMemberServiceImpl implements EmployeeFamilyMemberService {

    private final EmployeeFamilyMemberRepository familyMemberRepository;
    private final EmployeeRepository             employeeRepository;
    private final FamilyMemberMapper             familyMemberMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> list(UUID employeeId) {
        requireEmployee(employeeId);
        return familyMemberMapper.toResponseList(familyMemberRepository.findByEmployeeId(employeeId));
    }

    @Override
    @Transactional
    public FamilyMemberResponse add(UUID employeeId, FamilyMemberRequest request) {
        Employee employee = requireEmployee(employeeId);
        EmployeeFamilyMember member = familyMemberMapper.toEntity(request);
        member.setEmployee(employee);
        member = familyMemberRepository.save(member);
        return familyMemberMapper.toResponse(member);
    }

    @Override
    @Transactional
    public FamilyMemberResponse update(UUID employeeId, UUID memberId, FamilyMemberRequest request) {
        requireEmployee(employeeId);
        EmployeeFamilyMember member = familyMemberRepository.findByIdAndEmployeeId(memberId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member", "id", memberId.toString()));
        member.setName(request.getName());
        member.setRelationship(request.getRelationship());
        member.setDateOfBirth(request.getDateOfBirth());
        member.setGender(request.getGender());
        member.setOccupation(request.getOccupation());
        member.setContactNumber(request.getContactNumber());
        member = familyMemberRepository.save(member);
        return familyMemberMapper.toResponse(member);
    }

    @Override
    @Transactional
    public void delete(UUID employeeId, UUID memberId) {
        requireEmployee(employeeId);
        EmployeeFamilyMember member = familyMemberRepository.findByIdAndEmployeeId(memberId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member", "id", memberId.toString()));
        familyMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public List<FamilyMemberResponse> replaceAll(UUID employeeId, List<FamilyMemberRequest> requests) {
        Employee employee = requireEmployee(employeeId);
        familyMemberRepository.deleteByEmployeeId(employeeId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<EmployeeFamilyMember> members = requests.stream()
                .map(request -> {
                    EmployeeFamilyMember member = familyMemberMapper.toEntity(request);
                    member.setEmployee(employee);
                    return member;
                })
                .toList();
        return familyMemberMapper.toResponseList(familyMemberRepository.saveAll(members));
    }

    private Employee requireEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId.toString()));
    }
}
