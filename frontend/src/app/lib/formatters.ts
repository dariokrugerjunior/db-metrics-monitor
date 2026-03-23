export function formatNumber(value: number | null | undefined): string {
  return new Intl.NumberFormat("pt-BR").format(value ?? 0);
}

export function formatCompactNumber(value: number | null | undefined): string {
  return new Intl.NumberFormat("pt-BR", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(value ?? 0);
}

export function formatPercent(value: number | null | undefined, digits = 1): string {
  return `${(value ?? 0).toFixed(digits)}%`;
}

export function formatBytes(bytes: number | null | undefined): string {
  const value = bytes ?? 0;
  if (value === 0) return "0 B";

  const units = ["B", "KB", "MB", "GB", "TB"];
  const exponent = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
  const size = value / 1024 ** exponent;

  return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[exponent]}`;
}

export function bytesToMegabytes(bytes: number | null | undefined): number {
  return (bytes ?? 0) / 1024 ** 2;
}

export function formatMegabytes(bytes: number | null | undefined, digits = 2): string {
  return `${bytesToMegabytes(bytes).toFixed(digits)} MB`;
}

export function formatDuration(value: string | null | undefined): string {
  if (!value) return "0s";

  const normalized = value.startsWith("PT") ? value.slice(2) : value;
  const hoursMatch = normalized.match(/(\d+)H/);
  const minutesMatch = normalized.match(/(\d+)M/);
  const secondsMatch = normalized.match(/(\d+(?:\.\d+)?)S/);

  const hours = Number(hoursMatch?.[1] ?? 0);
  const minutes = Number(minutesMatch?.[1] ?? 0);
  const seconds = Math.floor(Number(secondsMatch?.[1] ?? 0));
  const parts: string[] = [];

  if (hours) parts.push(`${hours}h`);
  if (minutes) parts.push(`${minutes}m`);
  if (seconds || parts.length === 0) parts.push(`${seconds}s`);

  return parts.join(" ");
}

export function durationToMilliseconds(value: string | null | undefined): number {
  if (!value) return 0;

  const normalized = value.startsWith("PT") ? value.slice(2) : value;
  const hoursMatch = normalized.match(/(\d+)H/);
  const minutesMatch = normalized.match(/(\d+)M/);
  const secondsMatch = normalized.match(/(\d+(?:\.\d+)?)S/);

  const hours = Number(hoursMatch?.[1] ?? 0);
  const minutes = Number(minutesMatch?.[1] ?? 0);
  const seconds = Number(secondsMatch?.[1] ?? 0);

  return ((hours * 60 + minutes) * 60 + seconds) * 1000;
}

export function formatRelativeTimestamp(value: string | null | undefined): string {
  if (!value) return "N/A";

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(date);
}

export function buildFlatTrend(value: number, points = 7) {
  return Array.from({ length: points }, (_, index) => ({
    time: index === points - 1 ? "Agora" : `T-${points - index - 1}`,
    value,
  }));
}
