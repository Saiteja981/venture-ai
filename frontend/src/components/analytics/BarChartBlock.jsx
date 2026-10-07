import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Cell,
} from "recharts";
import EmptyChartState from "./EmptyChartState.jsx";

const COLORS = ["#4F46E5", "#06B6D4", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6"];

/**
 * Reusable bar chart block.
 * data: [{ label: string, value: number }, ...]
 */
export default function BarChartBlock({ data, dataKey = "value", labelKey = "label" }) {
    if (!data || data.length === 0) return <EmptyChartState />;

    return (
        <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
                <XAxis
                    dataKey={labelKey}
                    tick={{ fill: "#64748B", fontSize: 11 }}
                    axisLine={{ stroke: "#E2E8F0" }}
                    tickLine={false}
                    interval={0}
                    angle={data.length > 4 ? -20 : 0}
                    textAnchor={data.length > 4 ? "end" : "middle"}
                    height={data.length > 4 ? 60 : 30}
                />
                <YAxis
                    tick={{ fill: "#64748B", fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    allowDecimals={false}
                />
                <Tooltip
                    cursor={{ fill: "rgba(79, 70, 229, 0.06)" }}
                    contentStyle={{
                        borderRadius: 8,
                        border: "1px solid #E2E8F0",
                        fontSize: 12,
                    }}
                />
                <Bar dataKey={dataKey} radius={[6, 6, 0, 0]}>
                    {data.map((_, i) => (
                        <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}