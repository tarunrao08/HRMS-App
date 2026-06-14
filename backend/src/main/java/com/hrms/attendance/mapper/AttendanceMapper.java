package com.hrms.attendance.mapper;

import com.hrms.attendance.dto.AttendanceMonthlySummaryResponse;
import com.hrms.attendance.dto.AttendanceRecordResponse;
import com.hrms.attendance.dto.EmployeeShiftResponse;
import com.hrms.attendance.entity.AttendanceMonthlySummary;
import com.hrms.attendance.entity.AttendanceRecord;
import com.hrms.attendance.entity.EmployeeShift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName",
             expression = "java(record.getEmployee().getFirstName() + \" \" + record.getEmployee().getLastName())")
    @Mapping(source = "employee.employeeCode", target = "employeeCode")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.name", target = "shiftName")
    @Mapping(source = "createdAt", target = "createdAt")
    AttendanceRecordResponse toResponse(AttendanceRecord record);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName",
             expression = "java(summary.getEmployee().getFirstName() + \" \" + summary.getEmployee().getLastName())")
    @Mapping(source = "workingDays", target = "totalWorkingDays")
    AttendanceMonthlySummaryResponse toSummaryResponse(AttendanceMonthlySummary summary);

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeName",
             expression = "java(employeeShift.getEmployee().getFirstName() + \" \" + employeeShift.getEmployee().getLastName())")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.name", target = "shiftName")
    EmployeeShiftResponse toEmployeeShiftResponse(EmployeeShift employeeShift);
}
