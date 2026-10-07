export function deriveStartupFeatures(startup) {
    const monthlyRevenue = toNum(startup.monthlyRevenue);
    const customerCount = toNum(startup.customerCount);
    const totalUsers = toNum(startup.totalUsers);
    const teamSize = toNum(startup.teamSize);
    const runwayMonths = toNum(startup.runwayMonths);
    const revenueGrowthRate = toNum(startup.revenueGrowthRate);
    const totalFunding = toNum(startup.totalFunding);
    const monthlyBurnRate = toNum(startup.monthlyBurnRate);

    const revenuePerCustomer =
        customerCount > 0 ? monthlyRevenue / customerCount : 0;

    const usersPerTeamMember = teamSize > 0 ? totalUsers / teamSize : 0;

    const growthAdjustedRunway =
        runwayMonths * (1 + revenueGrowthRate / 100);

    const fundingToRevenue =
        monthlyRevenue > 0 ? totalFunding / monthlyRevenue : 0;

    const burnToRevenue =
        monthlyRevenue > 0 ? monthlyBurnRate / monthlyRevenue : 0;

    return {
        founded_year: toInt(startup.foundedYear) ?? 2000,
        team_size: toInt(teamSize) ?? 1,
        total_users: toInt(totalUsers) ?? 0,
        customer_count: toInt(customerCount) ?? 0,
        monthly_revenue: monthlyRevenue,
        revenue_growth_rate: revenueGrowthRate,
        total_funding: totalFunding,
        monthly_burn_rate: monthlyBurnRate,
        runway_months: runwayMonths,
        valuation: toNum(startup.valuation),
        revenue_per_customer: round2(revenuePerCustomer),
        users_per_team_member: round2(usersPerTeamMember),
        growth_adjusted_runway: round2(growthAdjustedRunway),
        funding_to_revenue: round2(fundingToRevenue),
        burn_to_revenue: round2(burnToRevenue),
        industry: startup.industry,
        business_model: startup.businessModel,
        startup_stage: startup.startupStage,
    };
}

function toNum(v) {
    if (v === null || v === undefined) return 0;
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
}

function toInt(v) {
    if (v === null || v === undefined || v === "") return null;
    const n = Number(v);
    return Number.isFinite(n) ? Math.round(n) : null;
}

function round2(n) {
    return Math.round(n * 100) / 100;
}