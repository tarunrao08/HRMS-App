package com.hrms.attendance.mapper;

import com.hrms.attendance.dto.ShiftRequest;
import com.hrms.attendance.dto.ShiftResponse;
import com.hrms.attendance.entity.Shift;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ShiftMapper {

    Shift toEntity(ShiftRequest request);

    ShiftResponse toResponse(Shift shift);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Shift shift, ShiftRequest request);
}
