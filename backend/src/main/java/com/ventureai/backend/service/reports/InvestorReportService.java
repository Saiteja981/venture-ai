package com.ventureai.backend.service.reports;

import com.lowagie.text.DocumentException;
import com.ventureai.backend.entity.CollaborationRequest;
import com.ventureai.backend.entity.CollaborationStatus;
import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.SavedStartup;
import com.ventureai.backend.entity.User;
import com.ventureai.backend.exception.ResourceNotFoundException;
import com.ventureai.backend.repository.CollaborationRequestRepository;
import com.ventureai.backend.repository.InvestorRepository;
import com.ventureai.backend.repository.SavedStartupRepository;
import com.ventureai.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class InvestorReportService {

    private final UserRepository userRepository;
    private final InvestorRepository investorRepository;
    private final SavedStartupRepository savedStartupRepository;
    private final CollaborationRequestRepository collaborationRepository;

    public InvestorReportService(UserRepository userRepository,
                                 InvestorRepository investorRepository,
                                 SavedStartupRepository savedStartupRepository,
                                 CollaborationRequestRepository collaborationRepository) {
        this.userRepository = userRepository;
        this.investorRepository = investorRepository;
        this.savedStartupRepository = savedStartupRepository;
        this.collaborationRepository = collaborationRepository;
    }

    /* ---------- Saved startups ---------- */

    @Transactional(readOnly = true)
    public ReportDownload savedStartups(String format) {
        Investor investor = currentInvestor();
        List<SavedStartup> saved =
                savedStartupRepository.findByInvestorIdOrderByCreatedAtDesc(investor.getId());

        if ("csv".equalsIgnoreCase(format)) {
            CsvBuilder b = new CsvBuilder()
                    .row("Saved At", "Company", "Industry", "Stage",
                            "Business Model", "Team Size", "Monthly Revenue",
                            "Total Funding", "Valuation");
            for (SavedStartup s : saved) {
                var st = s.getStartup();
                b.row(s.getCreatedAt(), st.getCompanyName(), st.getIndustry(),
                        st.getStartupStage(), st.getBusinessModel(), st.getTeamSize(),
                        st.getMonthlyRevenue(), st.getTotalFunding(), st.getValuation());
            }
            return ReportDownload.csv("investor-saved-startups", b.build());
        }

        try {
            PdfBuilder pdf = new PdfBuilder("Saved Startups",
                    "Bookmarks recorded against your investor profile.");
            if (saved.isEmpty()) {
                pdf.separator().note("You have not saved any startups yet.");
            } else {
                List<Object[]> rows = new ArrayList<>();
                for (SavedStartup s : saved) {
                    var st = s.getStartup();
                    rows.add(new Object[]{
                            st.getCompanyName(),
                            st.getIndustry(),
                            st.getStartupStage(),
                            st.getBusinessModel(),
                            st.getTeamSize(),
                            s.getCreatedAt(),
                    });
                }
                pdf.heading("Total: " + saved.size())
                        .table(new String[]{"Company", "Industry", "Stage",
                                "Business Model", "Team", "Saved At"}, rows);
            }
            pdf.separator()
                    .note("This report lists saved bookmarks only. No investment "
                            + "amounts, returns, or ROI are tracked or shown.")
                    .note("Not investment advice.");
            return ReportDownload.pdf("investor-saved-startups", pdf.close());
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }

    /* ---------- Portfolio (accepted collaborations) ---------- */

    @Transactional(readOnly = true)
    public ReportDownload portfolio(String format) {
        Investor investor = currentInvestor();
        List<CollaborationRequest> accepted = collaborationRepository
                .findByInvestorIdOrderByCreatedAtDesc(investor.getId()).stream()
                .filter(c -> c.getStatus() == CollaborationStatus.ACCEPTED)
                .toList();

        if ("csv".equalsIgnoreCase(format)) {
            CsvBuilder b = new CsvBuilder()
                    .row("Company", "Industry", "Stage", "Business Model",
                            "Team Size", "Monthly Revenue", "Total Funding",
                            "Valuation", "Collaboration ID", "Last Updated");
            for (CollaborationRequest c : accepted) {
                var st = c.getStartup();
                b.row(st.getCompanyName(), st.getIndustry(), st.getStartupStage(),
                        st.getBusinessModel(), st.getTeamSize(), st.getMonthlyRevenue(),
                        st.getTotalFunding(), st.getValuation(),
                        c.getId(), c.getUpdatedAt());
            }
            return ReportDownload.csv("investor-portfolio", b.build());
        }

        try {
            PdfBuilder pdf = new PdfBuilder("Collaboration Portfolio",
                    "Startups with an ACCEPTED collaboration request.");
            if (accepted.isEmpty()) {
                pdf.separator().note("You have no accepted collaborations yet.");
            } else {
                List<Object[]> rows = new ArrayList<>();
                for (CollaborationRequest c : accepted) {
                    var st = c.getStartup();
                    rows.add(new Object[]{
                            st.getCompanyName(),
                            st.getIndustry(),
                            st.getStartupStage(),
                            st.getTeamSize(),
                            c.getUpdatedAt(),
                    });
                }
                pdf.heading("Active collaborations: " + accepted.size())
                        .table(new String[]{"Company", "Industry", "Stage",
                                "Team", "Last Updated"}, rows);
            }
            pdf.separator()
                    .note("An accepted collaboration represents mutual interest — it is "
                            + "not a financial investment. No investment amounts, returns, "
                            + "or ROI are recorded.")
                    .note("Not investment advice.");
            return ReportDownload.pdf("investor-portfolio", pdf.close());
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to render PDF report.", e);
        }
    }

    /* ---------- helpers ---------- */

    private Investor currentInvestor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found"));
        return investorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not created an investor profile yet."));
    }
}