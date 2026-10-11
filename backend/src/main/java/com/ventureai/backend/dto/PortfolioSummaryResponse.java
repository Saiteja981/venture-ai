package com.ventureai.backend.dto;

import java.util.List;

public record PortfolioSummaryResponse(
        long activeCollaborations,
        long pendingCollaborations,
        long distinctIndustries,
        long savedStartups,
        List<PortfolioStartupResponse> startups
) {}