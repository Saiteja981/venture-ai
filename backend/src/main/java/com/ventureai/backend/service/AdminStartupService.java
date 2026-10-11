package com.ventureai.backend.service;

import com.ventureai.backend.dto.AdminStartupDetailResponse;
import com.ventureai.backend.dto.AdminStartupSummaryResponse;
import com.ventureai.backend.dto.PagedResponse;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.StartupRepository;
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
public class AdminStartupService {

    private final StartupRepository startupRepository;

    public AdminStartupService(StartupRepository startupRepository) {
        this.startupRepository = startupRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminStartupSummaryResponse> list(int page, int size,
                                                           String search,
                                                           String industry,
                                                           String stage) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Specification<Startup> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (industry != null && !industry.isBlank()) {
                ps.add(cb.equal(root.get("industry"), industry));
            }
            if (stage != null && !stage.isBlank()) {
                ps.add(cb.equal(root.get("startupStage"), stage));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<AdminStartupSummaryResponse> p = startupRepository.findAll(spec, pageable)
                .map(AdminStartupSummaryResponse::from);
        return PagedResponse.from(p);
    }

    @Transactional(readOnly = true)
    public AdminStartupDetailResponse get(Long id) {
        Startup s = startupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found"));
        return AdminStartupDetailResponse.from(s);
    }
}