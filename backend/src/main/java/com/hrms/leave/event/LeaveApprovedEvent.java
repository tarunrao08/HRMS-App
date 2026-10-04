package com.hrms.leave.event;

import com.hrms.leave.enums.HalfDayType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class LeaveApprovedEvent {
    private final UUID         employeeId;
    private final LocalDate    startDate;
    private final LocalDate    endDate;
    private final boolean      halfDay;
    private final HalfDayType  halfDayType;
}
