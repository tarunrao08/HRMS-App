package com.hrms.employee.mapper;

import com.hrms.employee.dto.BranchRequest;
import com.hrms.employee.dto.BranchResponse;
import com.hrms.employee.entity.Branch;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    Branch toEntity(BranchRequest request);

    BranchResponse toResponse(Branch branch);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Branch target, BranchRequest request);
}
