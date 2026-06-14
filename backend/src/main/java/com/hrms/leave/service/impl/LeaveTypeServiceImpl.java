package com.hrms.leave.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.dto.LeaveTypeResponse;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.mapper.LeaveTypeMapper;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.leave.service.LeaveTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveTypeServiceImpl implements LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveTypeMapper     leaveTypeMapper;

    @Override
    @Transactional
    public LeaveTypeResponse create(LeaveTypeRequest request) {
        if (leaveTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Leave type with name '" + request.getName() + "' already exists");
        }
        if (leaveTypeRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Leave type with code '" + request.getCode() + "' already exists");
        }
        LeaveType leaveType = leaveTypeMapper.toEntity(request);
        leaveType = leaveTypeRepository.save(leaveType);
        log.info("Created leave type: {} ({})", leaveType.getName(), leaveType.getCode());
        return leaveTypeMapper.toResponse(leaveType);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveTypeResponse getById(UUID id) {
        return leaveTypeMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getAll() {
        return leaveTypeRepository.findAll().stream()
                .map(leaveTypeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> getAllActive() {
        return leaveTypeRepository.findByActiveTrue().stream()
                .map(leaveTypeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveTypeResponse update(UUID id, LeaveTypeRequest request) {
        LeaveType leaveType = findOrThrow(id);

        boolean nameChanged = !leaveType.getName().equalsIgnoreCase(request.getName());
        if (nameChanged && leaveTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ValidationException("Leave type with name '" + request.getName() + "' already exists");
        }

        boolean codeChanged = !leaveType.getCode().equals(request.getCode());
        if (codeChanged && leaveTypeRepository.existsByCode(request.getCode())) {
            throw new ValidationException("Leave type with code '" + request.getCode() + "' already exists");
        }

        leaveTypeMapper.updateEntity(leaveType, request);
        leaveType = leaveTypeRepository.save(leaveType);
        log.info("Updated leave type: {} ({})", leaveType.getName(), leaveType.getCode());
        return leaveTypeMapper.toResponse(leaveType);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        LeaveType leaveType = findOrThrow(id);
        leaveTypeRepository.delete(leaveType);
        log.info("Deleted leave type: {} ({})", leaveType.getName(), leaveType.getCode());
    }

    private LeaveType findOrThrow(UUID id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", id.toString()));
    }
}
