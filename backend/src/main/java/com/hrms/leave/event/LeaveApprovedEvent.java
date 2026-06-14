package com.hrms.leave.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class LeaveApprovedEvent {
    private final UUID      employeeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
}
