import Card from "../common/Card.jsx";
import Badge from "../common/Badge.jsx";
import { Trophy } from "lucide-react";

export default function RecommendationCard({ recommendation }) {
    const score = Number(recommendation.compatibility_score) || 0;
    return (
        <Card hover className="flex items-center gap-4">
            <div className="flex h-11 w-11 flex-shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-600">
                <Trophy className="h-5 w-5" />
            </div>
            <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
          <span className="truncate text-sm font-semibold text-navy-800">
            {recommendation.startup_id}
          </span>
                    <Badge tone="navy">Rank #{recommendation.rank}</Badge>
                </div>
                <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
                    <div
                        className="h-full rounded-full bg-brand-600"
                        style={{ width: `${Math.max(0, Math.min(100, score))}%` }}
                    />
                </div>
            </div>
            <div className="text-right">
                <div className="text-lg font-semibold text-navy-800">
                    {score.toFixed(1)}
                </div>
                <div className="text-xs text-slate-400">match</div>
            </div>
        </Card>
    );
}