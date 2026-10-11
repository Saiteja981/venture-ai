package com.ventureai.backend.service;

import com.ventureai.backend.dto.AdminInvestorDetailResponse;
import com.ventureai.backend.dto.AdminInvestorSummaryResponse;
import com.ventureai.backend.dto.PagedResponse;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.InvestorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminInvestorService {

    private final InvestorRepository investorRepository;

    public AdminInvestorService(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminInvestorSummaryResponse> list(int page, int size,
                                                            String search,
                                                            String investorType,
                                                            String riskPreference) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<Investor> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("investorName")), like),
                        cb.like(cb.lower(root.get("organizationName")), like)
                ));
            }
            if (investorType != null && !investorType.isBlank()) {
                ps.add(cb.equal(root.get("investorType"), investorType));
            }
            if (riskPreference != null && !riskPreference.isBlank()) {
                ps.add(cb.equal(root.get("riskPreference"), riskPreference));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<AdminInvestorSummaryResponse> p = investorRepository.findAll(spec, pageable)
                .map(AdminInvestorSummaryResponse::from);
        return PagedResponse.from(p);
    }

    @Transactional(readOnly = true)
    public AdminInvestorDetailResponse get(Long id) {
        Investor i = investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor not found"));
        return AdminInvestorDetailResponse.from(i);
    }
}