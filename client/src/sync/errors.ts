function initError(error: Error, prototype: object): void {
  Object.setPrototypeOf(error, prototype);
}

export class SyncBusyError extends Error {
  constructor() {
    super("同步已经在进行");
    this.name = "SyncBusyError";
    initError(this, new.target.prototype);
  }
}

export class SyncRetryError extends Error {
  readonly delayMs: number;
  readonly cause: unknown;

  constructor(delayMs: number, cause: unknown) {
    super("同步没有完成，将按原 eventId 重试");
    this.name = "SyncRetryError";
    this.delayMs = delayMs;
    this.cause = cause;
    initError(this, new.target.prototype);
  }
}

export class SyncProtocolError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "SyncProtocolError";
    initError(this, new.target.prototype);
  }
}
