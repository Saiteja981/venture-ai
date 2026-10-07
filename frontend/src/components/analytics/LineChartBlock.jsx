import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from "recharts";
import EmptyChartState from "./EmptyChartState.jsx";

/**
 * Reusable line chart block.
 * data: [{ label: string, value: number }, ...]
 */
export default function LineChartBlock({ data, dataKey = "value", labelKey = "label", color = "#4F46E5" }) {
    if (!data || data.length === 0) return <EmptyChartState />;

    return (
        <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 12, left: 0, bottom: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" vertical={false} />
                <XAxis
                    dataKey={labelKey}
                    tick={{ fill: "#64748B", fontSize: 11 }}
                    axisLine={{ stroke: "#E2E8F0" }}
                    tickLine={false}
                />
                <YAxis
                    tick={{ fill: "#64748B", fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                    allowDecimals={false}
                />
                <Tooltip
                    contentStyle={{
                        borderRadius: 8,
                        border: "1px solid #E2E8F0",
                        fontSize: 12,
                    }}
                />
                <Line
                    type="monotone"
                    dataKey={dataKey}
                    stroke={color}
                    strokeWidth={2.5}
                    dot={{ r: 4, fill: color, strokeWidth: 0 }}
                    activeDot={{ r: 6 }}
                />
            </LineChart>
        </ResponsiveContainer>
    );
}