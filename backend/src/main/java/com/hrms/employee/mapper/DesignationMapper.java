package com.hrms.employee.mapper;

import com.hrms.employee.dto.DesignationRequest;
import com.hrms.employee.dto.DesignationResponse;
import com.hrms.employee.entity.Designation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DesignationMapper {

    @Mapping(target = "department", ignore = true)
    Designation toEntity(DesignationRequest request);

    @Mapping(source = "department.id",   target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    DesignationResponse toResponse(Designation designation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "department", ignore = true)
    void updateEntity(@MappingTarget Designation target, DesignationRequest request);
}
