package com.hrms.employee.mapper;

import com.hrms.employee.dto.BranchRequest;
import com.hrms.employee.dto.BranchResponse;
import com.hrms.employee.entity.Branch;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = DepartmentMapper.class)
public interface BranchMapper {

    // departmentIds needs a DB lookup to resolve to Department entities, which a pure mapper
    // can't do — BranchServiceImpl resolves and sets the department set explicitly.
    @Mapping(target = "departments", ignore = true)
    Branch toEntity(BranchRequest request);

    BranchResponse toResponse(Branch branch);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "departments", ignore = true)
    void updateEntity(@MappingTarget Branch target, BranchRequest request);
}
