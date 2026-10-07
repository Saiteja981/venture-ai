package com.ventureai.backend.service;

import com.ventureai.backend.dto.AdminStatsResponse;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.StartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;

    public AdminStatsService(UserRepository userRepository,
                             StartupRepository startupRepository,
                             InvestorRepository investorRepository) {
        this.userRepository = userRepository;
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalStartups = startupRepository.count();
        long totalInvestors = investorRepository.count();

        Map<String, Long> usersByRole = userRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        u -> u.getRole().name(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        long totalAdmins = usersByRole.getOrDefault(Role.ADMIN.name(), 0L);

        Map<String, Long> startupsByIndustry = startupRepository.findAll().stream()
                .filter(s -> s.getIndustry() != null)
                .collect(Collectors.groupingBy(
                        Startup::getIndustry,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> startupsByStage = startupRepository.findAll().stream()
                .filter(s -> s.getStartupStage() != null)
                .collect(Collectors.groupingBy(
                        Startup::getStartupStage,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Long> investorsByType = investorRepository.findAll().stream()
                .filter(i -> i.getInvestorType() != null)
                .collect(Collectors.groupingBy(
                        Investor::getInvestorType,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return new AdminStatsResponse(
                totalUsers, totalStartups, totalInvestors, totalAdmins,
                usersByRole, startupsByIndustry, startupsByStage, investorsByType
        );
    }
}