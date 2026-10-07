package com.ventureai.backend.repository;

import com.ventureai.backend.entity.Investor;
import com.ventureai.backend.entity.Role;
import com.ventureai.backend.entity.Startup;
import com.ventureai.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class PersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StartupRepository startupRepository;

    @Autowired
    private InvestorRepository investorRepository;

    // -----------------------------------------------------------------------
    // User persistence
    // -----------------------------------------------------------------------

    @Test
    void saveAndRetrieveUser() {
        User user = User.builder()
                .name("Alice")
                .email("alice@test.com")
                .password("hashed")
                .role(Role.STARTUP)
                .enabled(true)
                .build();
        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmail("alice@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void emailUniquenessEnforced() {
        userRepository.saveAndFlush(User.builder()
                .name("A").email("dup@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());

        User duplicate = User.builder()
                .name("B").email("dup@test.com").password("y")
                .role(Role.INVESTOR).enabled(true).build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmailWorks() {
        userRepository.saveAndFlush(User.builder()
                .name("A").email("exists@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());

        assertThat(userRepository.existsByEmail("exists@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("nope@test.com")).isFalse();
    }

    // -----------------------------------------------------------------------
    // Startup persistence
    // -----------------------------------------------------------------------

    @Test
    void saveAndRetrieveStartup() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("Owner").email("owner@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());

        Startup saved = startupRepository.saveAndFlush(Startup.builder()
                .user(owner)
                .companyName("Acme")
                .industry("AI")
                .businessModel("SaaS")
                .startupStage("SEED")
                .teamSize(5)
                .totalUsers(100L)
                .monthlyRevenue(BigDecimal.valueOf(1000))
                .revenueGrowthRate(BigDecimal.TEN)
                .totalFunding(BigDecimal.valueOf(50000))
                .monthlyBurnRate(BigDecimal.valueOf(2000))
                .runwayMonths(BigDecimal.valueOf(18))
                .customerCount(10L)
                .build());

        assertThat(saved.getId()).isNotNull();

        List<Startup> mine = startupRepository.findByUserId(owner.getId());
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getCompanyName()).isEqualTo("Acme");
    }

    @Test
    void findByIdAndUserIdOnlyReturnsOwned() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("O").email("o@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());
        User stranger = userRepository.saveAndFlush(User.builder()
                .name("S").email("s@test.com").password("y")
                .role(Role.STARTUP).enabled(true).build());

        Startup s = startupRepository.saveAndFlush(Startup.builder()
                .user(owner)
                .companyName("Acme").industry("AI").businessModel("SaaS")
                .startupStage("SEED").teamSize(5).totalUsers(100L)
                .monthlyRevenue(BigDecimal.ONE).revenueGrowthRate(BigDecimal.ONE)
                .totalFunding(BigDecimal.ONE).monthlyBurnRate(BigDecimal.ONE)
                .runwayMonths(BigDecimal.ONE).customerCount(1L).build());

        assertThat(startupRepository.findByIdAndUserId(s.getId(), owner.getId())).isPresent();
        assertThat(startupRepository.findByIdAndUserId(s.getId(), stranger.getId())).isEmpty();
    }

    @Test
    void startupUpdatedAtRefreshesOnUpdate() throws InterruptedException {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("O").email("upd@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());

        Startup s = startupRepository.saveAndFlush(Startup.builder()
                .user(owner)
                .companyName("Acme").industry("AI").businessModel("SaaS")
                .startupStage("SEED").teamSize(5).totalUsers(100L)
                .monthlyRevenue(BigDecimal.ONE).revenueGrowthRate(BigDecimal.ONE)
                .totalFunding(BigDecimal.ONE).monthlyBurnRate(BigDecimal.ONE)
                .runwayMonths(BigDecimal.ONE).customerCount(1L).build());

        LocalDateTime created = s.getCreatedAt();

        Thread.sleep(20);
        s.setCompanyName("Acme Updated");
        Startup updated = startupRepository.saveAndFlush(s);

        assertThat(updated.getCreatedAt()).isEqualTo(created);
        assertThat(updated.getUpdatedAt()).isAfter(created);
        assertThat(updated.getCompanyName()).isEqualTo("Acme Updated");
    }

    @Test
    void deleteStartupRemovesRow() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("O").email("del@test.com").password("x")
                .role(Role.STARTUP).enabled(true).build());

        Startup s = startupRepository.saveAndFlush(Startup.builder()
                .user(owner)
                .companyName("Acme").industry("AI").businessModel("SaaS")
                .startupStage("SEED").teamSize(5).totalUsers(100L)
                .monthlyRevenue(BigDecimal.ONE).revenueGrowthRate(BigDecimal.ONE)
                .totalFunding(BigDecimal.ONE).monthlyBurnRate(BigDecimal.ONE)
                .runwayMonths(BigDecimal.ONE).customerCount(1L).build());

        Long id = s.getId();
        startupRepository.delete(s);
        startupRepository.flush();

        assertThat(startupRepository.findById(id)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Investor persistence
    // -----------------------------------------------------------------------

    @Test
    void saveAndRetrieveInvestor() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("Inv").email("inv@test.com").password("x")
                .role(Role.INVESTOR).enabled(true).build());

        investorRepository.saveAndFlush(Investor.builder()
                .user(owner)
                .investorName("Jane")
                .investorType("VC_FIRM")
                .preferredIndustries("AI,FinTech")
                .preferredStages("SEED")
                .minimumInvestment(BigDecimal.valueOf(100_000))
                .maximumInvestment(BigDecimal.valueOf(2_000_000))
                .riskPreference("MEDIUM")
                .investmentExperienceYears(10)
                .build());

        Optional<Investor> found = investorRepository.findByUserId(owner.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getInvestorName()).isEqualTo("Jane");
    }

    @Test
    void investorUniquenessPerUser() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("Inv").email("inv2@test.com").password("x")
                .role(Role.INVESTOR).enabled(true).build());

        investorRepository.saveAndFlush(Investor.builder()
                .user(owner)
                .investorName("A").investorType("ANGEL")
                .preferredIndustries("AI").preferredStages("SEED")
                .minimumInvestment(BigDecimal.ONE).maximumInvestment(BigDecimal.TEN)
                .riskPreference("HIGH").investmentExperienceYears(3).build());

        Investor second = Investor.builder()
                .user(owner)
                .investorName("B").investorType("VC_FIRM")
                .preferredIndustries("SaaS").preferredStages("SERIES_A")
                .minimumInvestment(BigDecimal.ONE).maximumInvestment(BigDecimal.TEN)
                .riskPreference("LOW").investmentExperienceYears(5).build();

        assertThatThrownBy(() -> investorRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteInvestorRemovesProfile() {
        User owner = userRepository.saveAndFlush(User.builder()
                .name("Inv").email("inv3@test.com").password("x")
                .role(Role.INVESTOR).enabled(true).build());

        Investor investor = investorRepository.saveAndFlush(Investor.builder()
                .user(owner)
                .investorName("A").investorType("ANGEL")
                .preferredIndustries("AI").preferredStages("SEED")
                .minimumInvestment(BigDecimal.ONE).maximumInvestment(BigDecimal.TEN)
                .riskPreference("HIGH").investmentExperienceYears(3).build());

        investorRepository.delete(investor);
        investorRepository.flush();

        assertThat(investorRepository.findByUserId(owner.getId())).isEmpty();
    }
}