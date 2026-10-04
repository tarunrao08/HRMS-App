package com.hrms.employee.mapper;

import com.hrms.employee.dto.NomineeRequest;
import com.hrms.employee.dto.NomineeResponse;
import com.hrms.employee.entity.EmployeeNominee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NomineeMapper {

    @Mapping(target = "employee", ignore = true)
    EmployeeNominee toEntity(NomineeRequest request);

    @Mapping(source = "employee.id", target = "employeeId")
    NomineeResponse toResponse(EmployeeNominee entity);

    List<NomineeResponse> toResponseList(List<EmployeeNominee> entities);
}
