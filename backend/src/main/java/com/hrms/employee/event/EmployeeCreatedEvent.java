package com.hrms.employee.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class EmployeeCreatedEvent extends ApplicationEvent {

    private final UUID employeeId;

    public EmployeeCreatedEvent(Object source, UUID employeeId) {
        super(source);
        this.employeeId = employeeId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }
}
