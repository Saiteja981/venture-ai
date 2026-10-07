package com.ventureai.backend.service;

import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StartupService {

    private final StartupRepository startupRepository;
    private final UserRepository userRepository;

    public StartupService(StartupRepository startupRepository,
                          UserRepository userRepository) {
        this.startupRepository = startupRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Startup createStartup(Startup startup, String email) {
        User user = findUserByEmail(email);

        if (user.getRole() != Role.STARTUP) {
            throw new AccessDeniedException("Only STARTUP users can create startup profiles");
        }

        startup.setId(null);
        startup.setUser(user);
        startup.setCreatedAt(null);
        startup.setUpdatedAt(null);

        return startupRepository.save(startup);
    }

    @Transactional(readOnly = true)
    public List<Startup> getMyStartups(String email) {
        User user = findUserByEmail(email);
        return startupRepository.findByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public Startup getMyStartup(Long startupId, String email) {
        User user = findUserByEmail(email);
        return startupRepository.findByIdAndUserId(startupId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Startup not found or not owned by you"));
    }

    @Transactional
    public Startup updateMyStartup(Long startupId, Startup updatedStartup, String email) {
        User user = findUserByEmail(email);
        Startup startup = startupRepository.findByIdAndUserId(startupId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Startup not found or not owned by you"));

        startup.setCompanyName(updatedStartup.getCompanyName());
        startup.setDescription(updatedStartup.getDescription());
        startup.setIndustry(updatedStartup.getIndustry());
        startup.setBusinessModel(updatedStartup.getBusinessModel());
        startup.setStartupStage(updatedStartup.getStartupStage());
        startup.setFoundedYear(updatedStartup.getFoundedYear());
        startup.setTeamSize(updatedStartup.getTeamSize());
        startup.setTotalUsers(updatedStartup.getTotalUsers());
        startup.setMonthlyRevenue(updatedStartup.getMonthlyRevenue());
        startup.setRevenueGrowthRate(updatedStartup.getRevenueGrowthRate());
        startup.setTotalFunding(updatedStartup.getTotalFunding());
        startup.setMonthlyBurnRate(updatedStartup.getMonthlyBurnRate());
        startup.setRunwayMonths(updatedStartup.getRunwayMonths());
        startup.setValuation(updatedStartup.getValuation());
        startup.setCustomerCount(updatedStartup.getCustomerCount());
        startup.setWebsite(updatedStartup.getWebsite());
        startup.setPitchDeckUrl(updatedStartup.getPitchDeckUrl());

        return startupRepository.save(startup);
    }

    @Transactional
    public void deleteMyStartup(Long startupId, String email) {
        User user = findUserByEmail(email);
        Startup startup = startupRepository.findByIdAndUserId(startupId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Startup not found or not owned by you"));

        startupRepository.delete(startup);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}