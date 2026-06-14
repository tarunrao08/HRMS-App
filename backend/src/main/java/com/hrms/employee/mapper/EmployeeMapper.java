package com.hrms.employee.mapper;

import com.hrms.employee.dto.EmployeeRequest;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.EmployeeSummaryResponse;
import com.hrms.employee.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "department", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    Employee toEntity(EmployeeRequest request);

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "designation.id", target = "designationId")
    @Mapping(source = "designation.name", target = "designationTitle")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(expression = "java(employee.getManager() != null ? employee.getManager().getFirstName() + \" \" + employee.getManager().getLastName() : null)", target = "managerName")
    EmployeeResponse toResponse(Employee employee);

    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "designation.name", target = "designationTitle")
    @Mapping(source = "branch.name", target = "branchName")
    EmployeeSummaryResponse toSummary(Employee employee);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "designation", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    void updateEntity(@MappingTarget Employee target, EmployeeRequest request);
}
