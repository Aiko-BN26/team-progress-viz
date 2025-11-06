"use client";

import { useMemo, useState } from "react";
import { CalendarRange } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { ActivityPoint, OrganizationActivity } from "../../app/organizations/[id]/types";

type Props = {
  timezone: string;
  activity: OrganizationActivity;
};

const RANGE_OPTIONS = [
  { id: "daily", label: "日次" },
  { id: "weekly", label: "週次" },
] as const;

type RangeOption = (typeof RANGE_OPTIONS)[number]["id"];

export function ActivityChart({ timezone, activity }: Props) {
  const [range, setRange] = useState<RangeOption>("daily");

  const dataset = useMemo(() => {
    return range === "daily" ? activity.daily : activity.weekly;
  }, [activity.daily, activity.weekly, range]);

  const commitValues = dataset.map((point) => point.totalCommits);
  const activeValues = dataset.map((point) => point.activeMembers);
  const maxCommits = commitValues.length ? Math.max(...commitValues) : 0;
  const maxActive = activeValues.length ? Math.max(...activeValues) : 0;

  return (
    <Card className="h-full">
      <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <CardTitle className="text-base">活動トレンド</CardTitle>
          <p className="text-sm text-muted-foreground">
            アクティブ人数とコミット数の推移
          </p>
        </div>
        <div className="flex gap-2">
          {RANGE_OPTIONS.map((option) => (
            <Button
              key={option.id}
              size="sm"
              variant={range === option.id ? "default" : "outline"}
              onClick={() => setRange(option.id)}
            >
              <CalendarRange className="h-3.5 w-3.5" />
              {option.label}
            </Button>
          ))}
        </div>
      </CardHeader>
      <CardContent>
        {dataset.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            活動データがまだありません。
          </p>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-sm bg-emerald-500" />アクティブ人数（棒）
              </span>
              <span className="flex items-center gap-2">
                <span className="h-px w-4 bg-primary" />コミット数（線）
              </span>
            </div>
            <div className="overflow-x-auto">
              <MixedChart
                data={dataset}
                timezone={timezone}
                maxActive={maxActive}
                maxCommits={maxCommits}
              />
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

type MixedChartProps = {
  data: ActivityPoint[];
  timezone: string;
  maxActive: number;
  maxCommits: number;
};

function MixedChart({ data, timezone, maxActive, maxCommits }: MixedChartProps) {
  const chartHeight = 260;
  const padding = { top: 20, bottom: 36, left: 36, right: 48 };
  const innerHeight = chartHeight - padding.top - padding.bottom;
  const barWidth = 24;
  const groupGap = 20;
  const totalWidth =
    padding.left + padding.right + data.length * (barWidth + groupGap);

  const leftTicks = createTicks(maxActive);
  const rightTicks = createTicks(maxCommits);

  const labelFormatter = new Intl.DateTimeFormat("ja-JP", {
    month: "numeric",
    day: "numeric",
    timeZone: timezone,
  });

  const getActiveHeight = (value: number) =>
    maxActive === 0 ? 0 : (value / maxActive) * innerHeight;

  const getCommitY = (value: number) =>
    padding.top + innerHeight - (maxCommits === 0 ? 0 : (value / maxCommits) * innerHeight);

  return (
    <svg
      role="img"
      aria-label="アクティビティ棒・折れ線グラフ"
      className="h-[260px] min-w-[360px] w-full"
      viewBox={`0 0 ${totalWidth} ${chartHeight}`}
    >
      {leftTicks.map((tick) => {
        const y = padding.top + innerHeight - (maxActive === 0 ? 0 : (tick / maxActive) * innerHeight);
        return (
          <g key={`left-${tick}`}>
            <line
              x1={padding.left - 4}
              x2={totalWidth - padding.right}
              y1={y}
              y2={y}
              stroke="currentColor"
              className="text-muted-foreground/20"
              strokeDasharray="4 4"
            />
            <text
              x={padding.left - 8}
              y={y + 4}
              textAnchor="end"
              fontSize={10}
              className="fill-muted-foreground"
            >
              {tick}
            </text>
          </g>
        );
      })}

      {rightTicks.map((tick) => {
        const y = getCommitY(tick);
        return (
          <text
            key={`right-${tick}`}
            x={totalWidth - padding.right + 6}
            y={y + 4}
            fontSize={10}
            className="fill-muted-foreground"
          >
            {tick}
          </text>
        );
      })}

      <text x={padding.left} y={14} fontSize={10} className="fill-muted-foreground">
        アクティブ人数
      </text>
      <text
        x={totalWidth - padding.right}
        y={14}
        fontSize={10}
        textAnchor="end"
        className="fill-muted-foreground"
      >
        コミット数
      </text>

      {data.map((point, index) => {
        const barX = padding.left + index * (barWidth + groupGap);
        const barHeight = getActiveHeight(point.activeMembers);
        const barY = padding.top + innerHeight - barHeight;

        return (
          <g key={`${point.bucketStart}-bar`}>
            <rect
              x={barX}
              width={barWidth}
              y={barY}
              height={barHeight}
              rx={3}
              className="fill-emerald-500"
            />
            <text
              x={barX + barWidth / 2}
              y={chartHeight - 8}
              textAnchor="middle"
              fontSize={10}
              className="fill-muted-foreground"
            >
              {labelFormatter.format(new Date(point.bucketStart))}
            </text>
          </g>
        );
      })}

      {data.length > 0 && (
        <path
          d={data
            .map((point, index) => {
              const centerX = padding.left + index * (barWidth + groupGap) + barWidth / 2;
              const y = getCommitY(point.totalCommits);
              return `${index === 0 ? "M" : "L"}${centerX},${y}`;
            })
            .join(" ")}
          fill="none"
          strokeWidth={2}
          className="stroke-primary"
        />
      )}

      {data.map((point, index) => {
        const centerX = padding.left + index * (barWidth + groupGap) + barWidth / 2;
        const y = getCommitY(point.totalCommits);
        return (
          <circle
            key={`${point.bucketStart}-dot`}
            cx={centerX}
            cy={y}
            r={3}
            className="fill-primary"
          />
        );
      })}
    </svg>
  );
}

function createTicks(maxValue: number) {
  if (maxValue === 0) {
    return [0];
  }
  const step = Math.max(1, Math.ceil(maxValue / 4));
  const ticks: number[] = [];
  for (let value = 0; value <= maxValue; value += step) {
    ticks.push(value);
  }
  if (ticks[ticks.length - 1] !== maxValue) {
    ticks.push(maxValue);
  }
  return ticks;
}
