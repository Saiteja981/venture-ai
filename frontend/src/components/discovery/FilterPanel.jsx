import { useState } from "react";
import Card from "../common/Card.jsx";
import Button from "../common/Button.jsx";

const INDUSTRIES = [
    "Artificial Intelligence", "FinTech", "HealthTech", "EdTech",
    "E-Commerce", "SaaS", "Cybersecurity", "CleanTech", "Logistics", "Gaming",
];
const STAGES = ["PRE_SEED", "SEED", "SERIES_A", "SERIES_B"];
const BUSINESS_MODELS = [
    "B2B SaaS", "B2C SaaS", "Marketplace", "Subscription",
    "Freemium", "Enterprise", "Transactional",
];

const EMPTY = {
    industry: "",
    startupStage: "",
    businessModel: "",
    minFunding: "",
    maxFunding: "",
    minRevenue: "",
    maxRevenue: "",
    minTeamSize: "",
    maxTeamSize: "",
};

export default function FilterPanel({ onApply, onClear, disabled }) {
    const [filters, setFilters] = useState(EMPTY);

    function handleChange(e) {
        const { name, value } = e.target;
        setFilters((prev) => ({ ...prev, [name]: value }));
    }

    function handleApply(e) {
        e.preventDefault();
        onApply(filters);
    }

    function handleClear() {
        setFilters(EMPTY);
        onClear();
    }

    return (
        <Card>
            <form onSubmit={handleApply} className="space-y-4">
                <h3 className="text-base font-semibold text-navy-800">Filters</h3>

                <div>
                    <label className="block text-xs font-medium text-slate-600">Industry</label>
                    <select name="industry" value={filters.industry} onChange={handleChange}
                            className="input-field mt-1 text-sm">
                        <option value="">Any</option>
                        {INDUSTRIES.map((v) => <option key={v} value={v}>{v}</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-600">Startup Stage</label>
                    <select name="startupStage" value={filters.startupStage} onChange={handleChange}
                            className="input-field mt-1 text-sm">
                        <option value="">Any</option>
                        {STAGES.map((v) => <option key={v} value={v}>{v}</option>)}
                    </select>
                </div>

                <div>
                    <label className="block text-xs font-medium text-slate-600">Business Model</label>
                    <select name="businessModel" value={filters.businessModel} onChange={handleChange}
                            className="input-field mt-1 text-sm">
                        <option value="">Any</option>
                        {BUSINESS_MODELS.map((v) => <option key={v} value={v}>{v}</option>)}
                    </select>
                </div>

                <div className="grid grid-cols-2 gap-3">
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Min Funding</label>
                        <input name="minFunding" type="number" min={0}
                               value={filters.minFunding} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="0" />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Max Funding</label>
                        <input name="maxFunding" type="number" min={0}
                               value={filters.maxFunding} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="10000000" />
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Min Revenue</label>
                        <input name="minRevenue" type="number" min={0}
                               value={filters.minRevenue} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="0" />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Max Revenue</label>
                        <input name="maxRevenue" type="number" min={0}
                               value={filters.maxRevenue} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="1000000" />
                    </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Min Team</label>
                        <input name="minTeamSize" type="number" min={1}
                               value={filters.minTeamSize} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="1" />
                    </div>
                    <div>
                        <label className="block text-xs font-medium text-slate-600">Max Team</label>
                        <input name="maxTeamSize" type="number" min={1}
                               value={filters.maxTeamSize} onChange={handleChange}
                               className="input-field mt-1 text-sm" placeholder="500" />
                    </div>
                </div>

                <div className="flex gap-2 pt-2">
                    <Button type="submit" disabled={disabled} className="flex-1">
                        Apply Filters
                    </Button>
                    <Button type="button" variant="secondary" onClick={handleClear} disabled={disabled}>
                        Clear
                    </Button>
                </div>
            </form>
        </Card>
    );
}