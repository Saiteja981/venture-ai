package com.ventureai.backend.service;

import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestorService {

    private final InvestorRepository investorRepository;
    private final UserRepository userRepository;

    public InvestorService(InvestorRepository investorRepository,
                           UserRepository userRepository) {
        this.investorRepository = investorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Investor createInvestor(Investor investor, String email) {
        User user = findUserByEmail(email);

        if (user.getRole() != Role.INVESTOR) {
            throw new AccessDeniedException("Only INVESTOR users can create investor profiles");
        }

        if (investorRepository.existsByUserId(user.getId())) {
            throw new IllegalArgumentException(
                    "Investor profile already exists for this user");
        }

        investor.setId(null);
        investor.setUser(user);
        investor.setCreatedAt(null);
        investor.setUpdatedAt(null);

        return investorRepository.save(investor);
    }

    @Transactional(readOnly = true)
    public Investor getMyInvestorProfile(String email) {
        User user = findUserByEmail(email);
        return investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found"));
    }

    @Transactional
    public Investor updateMyInvestorProfile(Investor updatedInvestor, String email) {
        User user = findUserByEmail(email);
        Investor investor = investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found"));

        investor.setInvestorName(updatedInvestor.getInvestorName());
        investor.setOrganizationName(updatedInvestor.getOrganizationName());
        investor.setInvestorType(updatedInvestor.getInvestorType());
        investor.setPreferredIndustries(updatedInvestor.getPreferredIndustries());
        investor.setPreferredStages(updatedInvestor.getPreferredStages());
        investor.setPreferredBusinessModels(updatedInvestor.getPreferredBusinessModels());
        investor.setMinimumInvestment(updatedInvestor.getMinimumInvestment());
        investor.setMaximumInvestment(updatedInvestor.getMaximumInvestment());
        investor.setRiskPreference(updatedInvestor.getRiskPreference());
        investor.setInvestmentExperienceYears(updatedInvestor.getInvestmentExperienceYears());
        investor.setPortfolioSize(updatedInvestor.getPortfolioSize());
        investor.setPreferredLocation(updatedInvestor.getPreferredLocation());
        investor.setWebsite(updatedInvestor.getWebsite());
        investor.setBio(updatedInvestor.getBio());

        return investorRepository.save(investor);
    }

    @Transactional
    public void deleteMyInvestorProfile(String email) {
        User user = findUserByEmail(email);
        Investor investor = investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Investor profile not found"));

        investorRepository.delete(investor);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}