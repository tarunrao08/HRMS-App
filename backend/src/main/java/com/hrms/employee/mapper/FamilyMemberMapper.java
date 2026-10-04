package com.hrms.employee.mapper;

import com.hrms.employee.dto.FamilyMemberRequest;
import com.hrms.employee.dto.FamilyMemberResponse;
import com.hrms.employee.entity.EmployeeFamilyMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FamilyMemberMapper {

    @Mapping(target = "employee", ignore = true)
    EmployeeFamilyMember toEntity(FamilyMemberRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    FamilyMemberResponse toResponse(EmployeeFamilyMember entity);

    List<FamilyMemberResponse> toResponseList(List<EmployeeFamilyMember> entities);
}
