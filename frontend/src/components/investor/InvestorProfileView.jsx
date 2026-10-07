import { useState } from "react";
import FormField from "../common/FormField.jsx";
import SelectField from "../common/SelectField.jsx";
import TextareaField from "../common/TextareaField.jsx";
import Button from "../common/Button.jsx";

const INVESTOR_TYPES = [
    { value: "ANGEL", label: "Angel Investor" },
    { value: "VC_FIRM", label: "Venture Capital Firm" },
    { value: "CORPORATE", label: "Corporate Investor" },
    { value: "FAMILY_OFFICE", label: "Family Office" },
];

const RISK_PREFERENCES = [
    { value: "LOW", label: "Low" },
    { value: "MEDIUM", label: "Medium" },
    { value: "HIGH", label: "High" },
];

const STAGE_OPTIONS = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B"];
const INDUSTRY_OPTIONS = [
    "Artificial Intelligence", "FinTech", "HealthTech", "EdTech",
    "E-Commerce", "SaaS", "Cybersecurity", "CleanTech", "Logistics", "Gaming",
];
const BUSINESS_MODEL_OPTIONS = [
    "B2B SaaS", "B2C SaaS", "Marketplace", "Subscription",
    "Freemium", "Enterprise", "Transactional",
];

const EMPTY = {
    investorName: "",
    organizationName: "",
    investorType: "",
    preferredIndustries: "",
    preferredStages: "",
    preferredBusinessModels: "",
    minimumInvestment: "",
    maximumInvestment: "",
    riskPreference: "",
    investmentExperienceYears: "",
    portfolioSize: "",
    preferredLocation: "",
    website: "",
    bio: "",
};

function arrayFromCsv(s) {
    if (!s) return [];
    return String(s).split(",").map((x) => x.trim()).filter(Boolean);
}

function MultiSelect({ label, name, selected, options, onChange, required }) {
    const selectedSet = new Set(selected);

    function toggle(value) {
        const next = new Set(selectedSet);
        if (next.has(value)) next.delete(value);
        else next.add(value);
        onChange(name, Array.from(next));
    }

    return (
        <div>
            <label className="block text-sm font-medium text-slate-700">
                {label} {required && <span className="text-red-500">*</span>}
            </label>
            <div className="mt-2 flex flex-wrap gap-2">
                {options.map((opt) => {
                    const isOn = selectedSet.has(opt);
                    return (
                        <button
                            key={opt}
                            type="button"
                            onClick={() => toggle(opt)}
                            className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                                isOn
                                    ? "border-brand-500 bg-brand-50 text-brand-700"
                                    : "border-slate-300 bg-white text-slate-600 hover:bg-slate-50"
                            }`}
                        >
                            {opt}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}

export default function InvestorForm({
                                         initial,
                                         submitting,
                                         onSubmit,
                                         onCancel,
                                         submitLabel = "Save",
                                     }) {
    const [form, setForm] = useState(() => {
        const base = { ...EMPTY, ...(initial ?? {}) };
        base._industries = arrayFromCsv(initial?.preferredIndustries);
        base._stages = arrayFromCsv(initial?.preferredStages);
        base._businessModels = arrayFromCsv(initial?.preferredBusinessModels);
        return base;
    });
    const [errors, setErrors] = useState({});

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
        setErrors((prev) => ({ ...prev, [name]: undefined }));
    }

    function handleMulti(name, values) {
        setForm((prev) => ({ ...prev, [`_${name}`]: values }));
        setErrors((prev) => ({ ...prev, [name]: undefined }));
    }

    function validate() {
        const errs = {};
        if (!String(form.investorName).trim()) errs.investorName = "Investor name is required.";
        if (!String(form.investorType).trim()) errs.investorType = "Investor type is required.";
        if (!form._industries.length) errs.preferredIndustries = "Select at least one industry.";
        if (!form._stages.length) errs.preferredStages = "Select at least one stage.";
        if (form.minimumInvestment === "" || Number(form.minimumInvestment) < 0)
            errs.minimumInvestment = "Minimum investment is required.";
        if (form.maximumInvestment === "" || Number(form.maximumInvestment) < 0)
            errs.maximumInvestment = "Maximum investment is required.";
        if (!String(form.riskPreference).trim()) errs.riskPreference = "Risk preference is required.";
        if (form.investmentExperienceYears === "" || Number(form.investmentExperienceYears) < 0)
            errs.investmentExperienceYears = "Experience years is required.";
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
            investorName: form.investorName.trim(),
            organizationName: form.organizationName?.trim() || null,
            investorType: form.investorType,
            preferredIndustries: form._industries.join(","),
            preferredStages: form._stages.join(","),
            preferredBusinessModels: form._businessModels.length
                ? form._businessModels.join(",")
                : null,
            minimumInvestment: toNumber(form.minimumInvestment),
            maximumInvestment: toNumber(form.maximumInvestment),
            riskPreference: form.riskPreference,
            investmentExperienceYears: toNumber(form.investmentExperienceYears),
            portfolioSize: toNumber(form.portfolioSize),
            preferredLocation: form.preferredLocation?.trim() || null,
            website: form.website?.trim() || null,
            bio: form.bio?.trim() || null,
        };
        onSubmit(payload);
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-8" noValidate>
            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Investor Information
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField
                        label="Investor Name"
                        name="investorName"
                        value={form.investorName}
                        onChange={handleChange}
                        required
                        error={errors.investorName}
                        maxLength={150}
                    />
                    <FormField
                        label="Organization Name"
                        name="organizationName"
                        value={form.organizationName}
                        onChange={handleChange}
                        maxLength={150}
                    />
                    <SelectField
                        label="Investor Type"
                        name="investorType"
                        value={form.investorType}
                        onChange={handleChange}
                        options={INVESTOR_TYPES}
                        required
                        error={errors.investorType}
                    />
                </div>
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Investment Preferences
                </h3>
                <MultiSelect
                    label="Preferred Industries"
                    name="industries"
                    selected={form._industries}
                    options={INDUSTRY_OPTIONS}
                    onChange={handleMulti}
                    required
                />
                {errors.preferredIndustries && (
                    <p className="text-xs text-red-600">{errors.preferredIndustries}</p>
                )}
                <MultiSelect
                    label="Preferred Stages"
                    name="stages"
                    selected={form._stages}
                    options={STAGE_OPTIONS}
                    onChange={handleMulti}
                    required
                />
                {errors.preferredStages && (
                    <p className="text-xs text-red-600">{errors.preferredStages}</p>
                )}
                <MultiSelect
                    label="Preferred Business Models"
                    name="businessModels"
                    selected={form._businessModels}
                    options={BUSINESS_MODEL_OPTIONS}
                    onChange={handleMulti}
                />
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                    <SelectField
                        label="Risk Preference"
                        name="riskPreference"
                        value={form.riskPreference}
                        onChange={handleChange}
                        options={RISK_PREFERENCES}
                        required
                        error={errors.riskPreference}
                    />
                    <FormField
                        label="Minimum Investment (USD)"
                        name="minimumInvestment"
                        type="number"
                        step="0.01"
                        value={form.minimumInvestment}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.minimumInvestment}
                    />
                    <FormField
                        label="Maximum Investment (USD)"
                        name="maximumInvestment"
                        type="number"
                        step="0.01"
                        value={form.maximumInvestment}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.maximumInvestment}
                    />
                </div>
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Experience
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField
                        label="Investment Experience (years)"
                        name="investmentExperienceYears"
                        type="number"
                        value={form.investmentExperienceYears}
                        onChange={handleChange}
                        required
                        min={0}
                        error={errors.investmentExperienceYears}
                    />
                    <FormField
                        label="Portfolio Size"
                        name="portfolioSize"
                        type="number"
                        value={form.portfolioSize}
                        onChange={handleChange}
                        min={0}
                        helpText="Number of startups in your current portfolio"
                    />
                </div>
            </section>

            <section className="space-y-4">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
                    Additional Information
                </h3>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <FormField
                        label="Preferred Location"
                        name="preferredLocation"
                        value={form.preferredLocation}
                        onChange={handleChange}
                        maxLength={150}
                    />
                    <FormField
                        label="Website"
                        name="website"
                        type="url"
                        value={form.website}
                        onChange={handleChange}
                        placeholder="https://example.com"
                        maxLength={255}
                    />
                </div>
                <TextareaField
                    label="Bio"
                    name="bio"
                    value={form.bio}
                    onChange={handleChange}
                    rows={4}
                    maxLength={5000}
                    helpText="A short professional summary."
                />
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