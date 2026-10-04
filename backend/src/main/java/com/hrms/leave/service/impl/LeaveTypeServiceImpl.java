package com.hrms.leave.service.impl;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.exception.ValidationException;
import com.hrms.leave.dto.LeaveTypeRequest;
import com.hrms.leave.dto.LeaveTypeResponse;
import com.hrms.leave.dto.LeaveTypeTenureTierRequest;
import com.hrms.leave.dto.LeaveTypeTenureTierResponse;
import com.hrms.leave.entity.LeaveType;
import com.hrms.leave.entity.LeaveTypeTenureTier;
import com.hrms.leave.mapper.LeaveTypeMapper;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.leave.repository.LeaveTypeTenureTierRepository;
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

    private final LeaveTypeRepository           leaveTypeRepository;
    private final LeaveTypeTenureTierRepository leaveTypeTenureTierRepository;
    private final LeaveTypeMapper               leaveTypeMapper;

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

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeTenureTierResponse> getTiers(UUID leaveTypeId) {
        findOrThrow(leaveTypeId);
        return leaveTypeTenureTierRepository.findByLeaveTypeIdOrderByMinYearsAsc(leaveTypeId).stream()
                .map(leaveTypeMapper::toTierResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveTypeTenureTierResponse addTier(UUID leaveTypeId, LeaveTypeTenureTierRequest request) {
        LeaveType leaveType = findOrThrow(leaveTypeId);
        validateTierRequest(request);

        List<LeaveTypeTenureTier> existing = leaveTypeTenureTierRepository.findByLeaveTypeIdOrderByMinYearsAsc(leaveTypeId);
        rejectIfOverlapping(existing, request, null);

        LeaveTypeTenureTier tier = LeaveTypeTenureTier.builder()
                .leaveType(leaveType)
                .minYears(request.getMinYears())
                .maxYears(request.getMaxYears())
                .days(request.getDays())
                .build();
        tier = leaveTypeTenureTierRepository.save(tier);
        log.info("Added tenure tier to {}: {}-{} years -> {} days",
                leaveType.getCode(), request.getMinYears(), request.getMaxYears(), request.getDays());
        return leaveTypeMapper.toTierResponse(tier);
    }

    @Override
    @Transactional
    public LeaveTypeTenureTierResponse updateTier(UUID leaveTypeId, UUID tierId, LeaveTypeTenureTierRequest request) {
        findOrThrow(leaveTypeId);
        validateTierRequest(request);

        LeaveTypeTenureTier tier = leaveTypeTenureTierRepository.findById(tierId)
                .filter(t -> t.getLeaveType().getId().equals(leaveTypeId))
                .orElseThrow(() -> new ResourceNotFoundException("LeaveTypeTenureTier", "id", tierId.toString()));

        List<LeaveTypeTenureTier> existing = leaveTypeTenureTierRepository.findByLeaveTypeIdOrderByMinYearsAsc(leaveTypeId);
        rejectIfOverlapping(existing, request, tierId);

        tier.setMinYears(request.getMinYears());
        tier.setMaxYears(request.getMaxYears());
        tier.setDays(request.getDays());
        tier = leaveTypeTenureTierRepository.save(tier);
        return leaveTypeMapper.toTierResponse(tier);
    }

    @Override
    @Transactional
    public void deleteTier(UUID leaveTypeId, UUID tierId) {
        findOrThrow(leaveTypeId);
        LeaveTypeTenureTier tier = leaveTypeTenureTierRepository.findById(tierId)
                .filter(t -> t.getLeaveType().getId().equals(leaveTypeId))
                .orElseThrow(() -> new ResourceNotFoundException("LeaveTypeTenureTier", "id", tierId.toString()));
        leaveTypeTenureTierRepository.delete(tier);
    }

    private void validateTierRequest(LeaveTypeTenureTierRequest request) {
        if (request.getMaxYears() != null && request.getMaxYears() <= request.getMinYears()) {
            throw new ValidationException("Max years must be greater than min years (or left empty for unbounded)");
        }
    }

    private void rejectIfOverlapping(List<LeaveTypeTenureTier> existing, LeaveTypeTenureTierRequest request, UUID excludeTierId) {
        int newMin = request.getMinYears();
        int newMax = request.getMaxYears() != null ? request.getMaxYears() : Integer.MAX_VALUE;

        for (LeaveTypeTenureTier tier : existing) {
            if (excludeTierId != null && tier.getId().equals(excludeTierId)) continue;
            int otherMin = tier.getMinYears();
            int otherMax = tier.getMaxYears() != null ? tier.getMaxYears() : Integer.MAX_VALUE;
            boolean overlaps = newMin < otherMax && otherMin < newMax;
            if (overlaps) {
                throw new ValidationException(String.format(
                        "This range overlaps an existing tier (%d–%s years)",
                        tier.getMinYears(), tier.getMaxYears() == null ? "∞" : tier.getMaxYears()));
            }
        }
    }

    private LeaveType findOrThrow(UUID id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", id.toString()));
    }
}
