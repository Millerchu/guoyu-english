function initError(error: Error, prototype: object): void {
  Object.setPrototypeOf(error, prototype);
}

export class DeviceRevokedError extends Error {
  readonly code = "DEVICE_REVOKED";

  constructor() {
    super("设备已撤销，停止同步");
    this.name = "DeviceRevokedError";
    initError(this, new.target.prototype);
  }
}

export class RetryAfterError extends Error {
  readonly delayMs: number;

  constructor(delayMs: number) {
    super("同步被限流");
    this.name = "RetryAfterError";
    this.delayMs = delayMs;
    initError(this, new.target.prototype);
  }
}

export class NetworkError extends Error {
  constructor() {
    super("网络请求失败");
    this.name = "NetworkError";
    initError(this, new.target.prototype);
  }
}

export class ApiResponseError extends Error {
  readonly status: number;
  readonly apiCode: string;
  readonly requestId: string;

  constructor(status: number, apiCode: string, requestId: string) {
    super(`同步请求失败：${apiCode}`);
    this.name = "ApiResponseError";
    this.status = status;
    this.apiCode = apiCode;
    this.requestId = requestId;
    initError(this, new.target.prototype);
  }
}

export class SyncRejectedError extends Error {
  readonly retryable = false;
  readonly reason: string;

  constructor(reason: string) {
    super("同步被拒绝，原 eventId 仍保留");
    this.name = "SyncRejectedError";
    this.reason = reason;
    initError(this, new.target.prototype);
  }
}
