import { useState } from "react";
import FormField from "../common/FormField.jsx";
import SelectField from "../common/SelectField.jsx";
import TextareaField from "../common/TextareaField.jsx";
import Button from "../common/Button.jsx";

const INDUSTRY_OPTIONS = [
    "Artificial Intelligence", "FinTech", "HealthTech", "EdTech",
    "E-Commerce", "SaaS", "Cybersecurity", "CleanTech", "Logistics", "Gaming",
];

const BUSINESS_MODEL_OPTIONS = [
    "B2B SaaS", "B2C SaaS", "Marketplace", "Subscription",
    "Freemium", "Enterprise", "Transactional",
];

const STAGE_OPTIONS = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B"];

const EMPTY = {
    companyName: "",
    description: "",
    industry: "",
    businessModel: "",
    startupStage: "",
    foundedYear: "",
    teamSize: "",
    totalUsers: "",
    monthlyRevenue: "",
    revenueGrowthRate: "",
    totalFunding: "",
    monthlyBurnRate: "",
    runwayMonths: "",
    valuation: "",
    customerCount: "",
    website: "",
    pitchDeckUrl: "",
};

export default function StartupForm({
                                        initial,
                                        submitting,
                                        onSubmit,
                                        onCancel,
                                        submitLabel = "Save",
                                    }) {
    const [form, setForm] = useState(() => ({ ...EMPTY, ...(initial ?? {}) }));
    const [errors, setErrors] = useState({});

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
        setErrors((prev) => ({ ...prev, [name]: undefined }));
    }

    function validate() {
        const errs = {};
        if (!String(form.companyName).trim()) errs.companyName = "Company name is required.";
        if (!String(form.industry).trim()) errs.industry = "Industry is required.";
        if (!String(form.businessModel).trim()) errs.businessModel = "Business model is required.";
        if (!String(form.startupStage).trim()) errs.startupStage = "Startup stage is required.";
        if (form.teamSize === "" || Number(form.teamSize) < 1) errs.teamSize = "Team size must be at least 1.";
        if (form.totalUsers === "" || Number(form.totalUsers) < 0) errs.totalUsers = "Total users must be 0 or more.";
        if (form.monthlyRevenue === "" || Number(form.monthlyRevenue) < 0) errs.monthlyRevenue = "Monthly revenue is required.";
        if (form.revenueGrowthRate === "") errs.revenueGrowthRate = "Revenue growth rate is required.";
        if (form.totalFunding === "" || Number(form.totalFunding) < 0) errs.totalFunding = "Total funding is required.";
        if (form.monthlyBurnRate === "" || Number(form.monthlyBurnRate) < 0) errs.monthlyBurnRate = "Monthly burn rate is required.";
        if (form.runwayMonths === "" || Number(form.runwayMonths) < 0) errs.runwayMonths = "Runway months is required.";
        if (form.customerCount === "" || Number(form.customerCount) < 0) errs.customerCount = "Customer count is required.";
        setErrors(errs);
        return Object.keys(errs).length === 0;
    }

    function toNumber(v) {
        if (v === "" || v === null || v === undefined) return null;
        const n = Number(v);
        return Number.isNaN(n) ? null : n;
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!validate()) return;

        const payload = {
            companyName: form.companyName.trim(),
            description: form.description?.trim() || null,
            industry: form.industry,
            businessModel: form.businessModel,
            startupStage: form.startupStage,
            foundedYear: toNumber(form.foundedYear),
            teamSize: toNumber(form.teamSize),
            totalUsers: toNumber(form.totalUsers),
            monthlyRevenue: toNumber(form.monthlyRevenue),
            revenueGrowthRate: toNumber(form.revenueGrowthRate),
            totalFunding: toNumber(form.totalFunding),
            monthlyBurnRate: toNumber(form.monthlyBurnRate),
            runwayMonths: toNumber(form.runwayMonths),
            valuation: toNumber(form.valuation),
            customerCount: toNumber(form.customerCount),
            website: form.website?.trim() || null,
            pitchDeckUrl: form.pitchDeckUrl?.trim() || null,
        };
        onSubmit(payload);
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-8" noValidate>
            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Basic Information
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField
                        label="Company Name"
                        name="companyName"
                        value={form.companyName}
                        onChange={handleChange}
                        required
                        error={errors.companyName}
                        maxLength={150}
                    />
                    <SelectField
                        label="Industry"
                        name="industry"
                        value={form.industry}
                        onChange={handleChange}
                        options={INDUSTRY_OPTIONS.map((i) => ({ value: i, label: i }))}
                        required
                        error={errors.industry}
                    />
                    <SelectField
                        label="Business Model"
                        name="businessModel"
                        value={form.businessModel}
                        onChange={handleChange}
                        options={BUSINESS_MODEL_OPTIONS.map((i) => ({ value: i, label: i }))}
                        required
                        error={errors.businessModel}
                    />
                    <SelectField
                        label="Startup Stage"
                        name="startupStage"
                        value={form.startupStage}
                        onChange={handleChange}
                        options={STAGE_OPTIONS.map((i) => ({ value: i, label: i }))}
                        required
                        error={errors.startupStage}
                    />
                </div>
                <TextareaField
                    label="Description"
                    name="description"
                    value={form.description}
                    onChange={handleChange}
                    rows={4}
                    maxLength={5000}
                    helpText="Briefly describe what your startup does."
                />
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Company Metrics
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <FormField
                        label="Founded Year"
                        name="foundedYear"
                        type="number"
                        value={form.foundedYear}
                        onChange={handleChange}
                        min={1900}
                        max={2100}
                    />
                    <FormField
                        label="Team Size"
                        name="teamSize"
                        type="number"
                        value={form.teamSize}
                        onChange={handleChange}
                        required
                        min={1}
                        error={errors.teamSize}
                    />
                    <FormField
                        label="Total Users"
                        name="totalUsers"
                        type="number"
                        value={form.totalUsers}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.totalUsers}
                    />
                    <FormField
                        label="Customer Count"
                        name="customerCount"
                        type="number"
                        value={form.customerCount}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.customerCount}
                    />
                </div>
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Financial Information
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <FormField
                        label="Monthly Revenue (USD)"
                        name="monthlyRevenue"
                        type="number"
                        step="0.01"
                        value={form.monthlyRevenue}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.monthlyRevenue}
                    />
                    <FormField
                        label="Revenue Growth Rate (%)"
                        name="revenueGrowthRate"
                        type="number"
                        step="0.01"
                        value={form.revenueGrowthRate}
                        onChange={handleChange}
                        required
                        error={errors.revenueGrowthRate}
                    />
                    <FormField
                        label="Total Funding (USD)"
                        name="totalFunding"
                        type="number"
                        step="0.01"
                        value={form.totalFunding}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.totalFunding}
                    />
                    <FormField
                        label="Monthly Burn Rate (USD)"
                        name="monthlyBurnRate"
                        type="number"
                        step="0.01"
                        value={form.monthlyBurnRate}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.monthlyBurnRate}
                    />
                    <FormField
                        label="Runway (months)"
                        name="runwayMonths"
                        type="number"
                        step="0.01"
                        value={form.runwayMonths}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.runwayMonths}
                    />
                    <FormField
                        label="Valuation (USD)"
                        name="valuation"
                        type="number"
                        step="0.01"
                        value={form.valuation}
                        onChange={handleChange}
                        min={0}
                    />
                </div>
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Links
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField
                        label="Website"
                        name="website"
                        type="url"
                        value={form.website}
                        onChange={handleChange}
                        placeholder="https://example.com"
                        maxLength={255}
                    />
                    <FormField
                        label="Pitch Deck URL"
                        name="pitchDeckUrl"
                        type="url"
                        value={form.pitchDeckUrl}
                        onChange={handleChange}
                        placeholder="https://example.com/pitch-deck.pdf"
                        maxLength={500}
                    />
                </div>
            </section>

            <div className="flex items-center justify-end gap-2 border-t border-slate-200 pt-5">
                {onCancel && (
                    <Button variant="secondary" onClick={onCancel} disabled={submitting}>
                        Cancel
                    </Button>
                )}
                <Button type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : submitLabel}
                </Button>
            </div>
        </form>
    );
}