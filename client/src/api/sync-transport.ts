import { ApiResponseError, DeviceRevokedError, NetworkError, RetryAfterError, SyncRejectedError } from "./errors";
import {
  API_CODES,
  type ApiCode,
  type SyncPullQuery,
  type SyncPullResponseData,
  type SyncPushEventResult,
  type SyncPushRequest,
  type SyncPushResponseData,
} from "./sync-types";
import { encodeSyncPushRequest } from "./sync-json";
import { InvalidArgumentError } from "../repositories/errors";
import { parseToken } from "../repositories/record-parse";
import type { SyncChange } from "../repositories/types";

export interface FetchInit {
  readonly method: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly body?: string;
}

export interface ResponseHeaderReader {
  get: (name: string) => string | null;
}

export interface FetchResponse {
  readonly ok: boolean;
  readonly status: number;
  readonly headers: ResponseHeaderReader;
  text: () => Promise<string>;
}

export type FetchLike = (input: string, init: FetchInit) => Promise<FetchResponse>;

export interface SyncTransport {
  push(request: SyncPushRequest): Promise<SyncPushResponseData>;
  pull(query: SyncPullQuery): Promise<SyncPullResponseData>;
}

export interface SyncHttpTransportOptions {
  readonly baseUrl?: string;
  readonly fetchImpl?: FetchLike;
  /** 原始设备令牌，不写入错误信息。 */
  readonly deviceToken?: string;
}

const RESPONSE_TEXT_MAX = 1024 * 1024;

export function createSyncHttpTransport(options: SyncHttpTransportOptions = {}): SyncTransport {
  const fetchImpl = options.fetchImpl ?? defaultFetch;
  const baseUrl = options.baseUrl ?? "";
  return {
    push(request: SyncPushRequest): Promise<SyncPushResponseData> {
      return postPush(fetchImpl, baseUrl, options.deviceToken, request);
    },
    pull(query: SyncPullQuery): Promise<SyncPullResponseData> {
      return getPull(fetchImpl, baseUrl, options.deviceToken, query);
    },
  };
}

export function retryAfterDelayMs(header: string | null): number | null {
  if (header === null) {
    return null;
  }
  const trimmed = header.trim();
  if (!/^\d+$/.test(trimmed)) {
    return null;
  }
  const seconds = Number(trimmed);
  if (!Number.isSafeInteger(seconds)) {
    return null;
  }
  return seconds * 1000;
}

async function postPush(
  fetchImpl: FetchLike,
  baseUrl: string,
  deviceToken: string | undefined,
  request: SyncPushRequest,
): Promise<SyncPushResponseData> {
  const body = encodeSyncPushRequest(request);
  const response = await send(fetchImpl, joinUrl(baseUrl, "/api/v1/sync/push"), {
    method: "POST",
    headers: headers(deviceToken),
    body,
  });
  const data = await readData(response);
  return parsePushData(data);
}

async function getPull(
  fetchImpl: FetchLike,
  baseUrl: string,
  deviceToken: string | undefined,
  query: SyncPullQuery,
): Promise<SyncPullResponseData> {
  validatePullQuery(query);
  const params = new URLSearchParams({
    childId: query.childId,
    streamEpoch: query.streamEpoch,
    cursor: query.cursor,
    limit: String(query.limit),
  });
  const response = await send(fetchImpl, `${joinUrl(baseUrl, "/api/v1/sync/pull")}?${params.toString()}`, {
    method: "GET",
    headers: headers(deviceToken),
  });
  const data = await readData(response);
  return parsePullData(data);
}

function headers(deviceToken: string | undefined): Record<string, string> {
  const result: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
    "X-Request-Id": createRequestId(),
  };
  if (deviceToken !== undefined && deviceToken.length > 0) {
    result.Authorization = `Bearer ${deviceToken}`;
  }
  return result;
}

function createRequestId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return "request-id-unavailable";
}

async function send(fetchImpl: FetchLike, url: string, init: FetchInit): Promise<FetchResponse> {
  try {
    return await fetchImpl(url, init);
  } catch {
    throw new NetworkError();
  }
}

async function readData(response: FetchResponse): Promise<unknown> {
  if (response.status === 429) {
    const delay = retryAfterDelayMs(response.headers.get("retry-after"));
    throw new RetryAfterError(delay ?? 5000);
  }
  if (response.status === 401) {
    throw new DeviceRevokedError();
  }
  if (response.status === 413) {
    throw new SyncRejectedError("HTTP_413");
  }
  const payload = await readJson(response);
  const envelope = parseEnvelope(payload, response.status);
  if (envelope.code === "DEVICE_REVOKED") {
    throw new DeviceRevokedError();
  }
  if (!response.ok || envelope.code !== "OK") {
    if (envelope.code === "INVALID_ARGUMENT" || envelope.code === "EVENT_CONFLICT" || envelope.code === "EVENT_ID_REUSED") {
      throw new SyncRejectedError(envelope.code);
    }
    throw new ApiResponseError(response.status, envelope.code, envelope.requestId);
  }
  return envelope.data;
}

async function readJson(response: FetchResponse): Promise<unknown> {
  let text = "";
  try {
    text = await response.text();
  } catch {
    throw new ApiResponseError(response.status, "INTERNAL_ERROR", "");
  }
  if (text.length > RESPONSE_TEXT_MAX) {
    throw new ApiResponseError(response.status, "INTERNAL_ERROR", "");
  }
  try {
    return parseJson(text);
  } catch {
    throw new ApiResponseError(response.status, "INTERNAL_ERROR", "");
  }
}

function parseJson(text: string): unknown {
  return JSON.parse(text) as unknown;
}

function parseEnvelope(payload: unknown, status: number): { requestId: string; code: ApiCode; data: unknown } {
  if (!isRecord(payload)) {
    throw new ApiResponseError(status, "INTERNAL_ERROR", "");
  }
  const requestId = payload.requestId;
  const code = payload.code;
  const message = payload.message;
  const serverTime = payload.serverTime;
  if (typeof requestId !== "string" || typeof message !== "string" || typeof serverTime !== "string") {
    throw new ApiResponseError(status, "INTERNAL_ERROR", "");
  }
  if (!isApiCode(code)) {
    throw new ApiResponseError(status, "INTERNAL_ERROR", requestId);
  }
  if (!serverTime.endsWith("Z") || !serverTime.includes("T")) {
    throw new ApiResponseError(status, "INTERNAL_ERROR", requestId);
  }
  return { requestId, code, data: payload.data };
}

function parsePushData(data: unknown): SyncPushResponseData {
  if (!isRecord(data) || !Array.isArray(data.results)) {
    throw new SyncRejectedError("BAD_RESPONSE");
  }
  const results: SyncPushEventResult[] = [];
  for (const item of data.results) {
    results.push(parsePushResult(item));
  }
  return { results };
}

function parsePushResult(item: unknown): SyncPushEventResult {
  if (!isRecord(item) || typeof item.eventId !== "string") {
    throw new SyncRejectedError("BAD_RESPONSE");
  }
  if (item.status !== "ACCEPTED" && item.status !== "DUPLICATE" && item.status !== "REJECTED") {
    throw new SyncRejectedError("BAD_RESPONSE");
  }
  const result: SyncPushEventResult = {
    eventId: item.eventId,
    status: item.status,
  };
  if (item.code === undefined) {
    return result;
  }
  if (!isApiCode(item.code)) {
    throw new SyncRejectedError("BAD_RESPONSE");
  }
  return { ...result, code: item.code };
}

function parsePullData(data: unknown): SyncPullResponseData {
  if (!isRecord(data) || !Array.isArray(data.changes) || typeof data.nextCursor !== "string" || typeof data.hasMore !== "boolean") {
    throw new SyncRejectedError("BAD_RESPONSE");
  }
  const changes: Record<string, unknown>[] = [];
  for (const change of data.changes) {
    if (!isRecord(change)) {
      throw new SyncRejectedError("BAD_RESPONSE");
    }
    changes.push(change);
  }
  return {
    changes: asSyncChanges(changes),
    nextCursor: data.nextCursor,
    hasMore: data.hasMore,
  };
}

/**
 * 契约没有列出 Pull 条目字段。这里保留服务端 JSON，
 * 只为了满足仓储参数的现有类型。
 */
function asSyncChanges(changes: readonly Record<string, unknown>[]): readonly SyncChange[] {
  return changes as unknown as readonly SyncChange[];
}

function isApiCode(value: unknown): value is ApiCode {
  return typeof value === "string" && API_CODES.some((code) => code === value);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function validatePullQuery(query: SyncPullQuery): void {
  parseToken(query.childId, "childId", 64);
  parseToken(query.streamEpoch, "streamEpoch", 80);
  if (query.cursor !== "") {
    parseToken(query.cursor, "cursor", 256);
  }
  if (!Number.isSafeInteger(query.limit) || query.limit < 1 || query.limit > 500) {
    throw new InvalidArgumentError("limit");
  }
}

function joinUrl(baseUrl: string, path: string): string {
  if (baseUrl.length === 0) {
    return path;
  }
  const trimmed = baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
  return `${trimmed}${path}`;
}

const defaultFetch: FetchLike = (input, init) => {
  return fetch(input, init);
};
