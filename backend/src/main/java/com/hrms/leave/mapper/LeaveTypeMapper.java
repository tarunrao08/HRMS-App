package com.hrms.leave.mapper;

import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.dto.LeaveTypeResponse;
import com.hrms.leave.entity.LeaveType;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LeaveTypeMapper {

    LeaveType toEntity(LeaveTypeRequest request);

    LeaveTypeResponse toResponse(LeaveType leaveType);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget LeaveType target, LeaveTypeRequest request);
}
