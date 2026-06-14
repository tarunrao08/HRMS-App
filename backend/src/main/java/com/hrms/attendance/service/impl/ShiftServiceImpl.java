package com.hrms.attendance.service.impl;

import com.hrms.attendance.dto.ShiftRequest;
import com.hrms.attendance.dto.ShiftResponse;
import com.hrms.attendance.entity.Shift;
import com.hrms.attendance.mapper.ShiftMapper;
import com.hrms.attendance.repository.ShiftRepository;
import com.hrms.attendance.service.ShiftService;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftMapper shiftMapper;

    @Override
    public ShiftResponse create(ShiftRequest request) {
        if (shiftRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Shift with name '" + request.getName() + "' already exists");
        }
        Shift shift = shiftMapper.toEntity(request);
        Shift saved = shiftRepository.save(shift);
        log.info("Created shift id={} name={}", saved.getId(), saved.getName());
        return shiftMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getById(UUID id) {
        Shift shift = findShiftOrThrow(id);
        return shiftMapper.toResponse(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getAll() {
        return shiftRepository.findAll().stream()
                .map(shiftMapper::toResponse)
                .toList();
    }

    @Override
    public ShiftResponse update(UUID id, ShiftRequest request) {
        Shift shift = findShiftOrThrow(id);
        if (!shift.getName().equalsIgnoreCase(request.getName())
                && shiftRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Shift with name '" + request.getName() + "' already exists");
        }
        shiftMapper.updateEntity(shift, request);
        Shift saved = shiftRepository.save(shift);
        log.info("Updated shift id={}", saved.getId());
        return shiftMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        Shift shift = findShiftOrThrow(id);
        shiftRepository.delete(shift);
        log.info("Deleted shift id={}", id);
    }

    @Override
    public ShiftResponse toggleActive(UUID id) {
        Shift shift = findShiftOrThrow(id);
        shift.setActive(!shift.isActive());
        Shift saved = shiftRepository.save(shift);
        log.info("Toggled shift id={} active={}", saved.getId(), saved.isActive());
        return shiftMapper.toResponse(saved);
    }

    private Shift findShiftOrThrow(UUID id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", "id", id.toString()));
    }
}
