package com.hrms.attendance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PunchRequest {

    private String location;
    private String ip;
    private String remarks;
}
