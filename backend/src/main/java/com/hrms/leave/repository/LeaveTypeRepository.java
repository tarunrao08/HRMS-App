package com.hrms.leave.repository;

import com.hrms.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCode(String code);

    List<LeaveType> findByActiveTrue();
}
