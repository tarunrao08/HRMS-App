package com.hrms.attendance.repository;

import com.hrms.attendance.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
