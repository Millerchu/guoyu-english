/** 5s、15s、45s，最长 5 分钟。抖动只向下，结果不超过该档。 */
export const SYNC_BACKOFF_MS = [5_000, 15_000, 45_000, 300_000] as const;

export function backoffDelayMs(attemptIndex: number, randomUnit: number): number {
  if (!Number.isInteger(attemptIndex) || attemptIndex < 0) {
    throw new Error("重试次数无效");
  }
  if (!Number.isFinite(randomUnit) || randomUnit < 0 || randomUnit >= 1) {
    throw new Error("抖动随机数无效");
  }
  const base = SYNC_BACKOFF_MS[Math.min(attemptIndex, SYNC_BACKOFF_MS.length - 1)] ?? 300_000;
  const jitter = Math.floor(base * 0.2 * randomUnit);
  return base - jitter;
}
