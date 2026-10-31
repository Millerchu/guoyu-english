function initError(error: Error, prototype: object): void {
  Object.setPrototypeOf(error, prototype);
}

export class InvalidArgumentError extends Error {
  readonly saved = false;

  constructor(field: string) {
    super(`${field} 无效`);
    this.name = "InvalidArgumentError";
    initError(this, new.target.prototype);
  }
}

export class InvalidLearningActionError extends Error {
  readonly saved = false;

  constructor(message: string) {
    super(message);
    this.name = "InvalidLearningActionError";
    initError(this, new.target.prototype);
  }
}

export class EventIdReusedError extends Error {
  readonly saved = false;

  constructor() {
    super("eventId 已绑定另一条学习动作，没有改写，也没有新编号");
    this.name = "EventIdReusedError";
    initError(this, new.target.prototype);
  }
}

export class LearningActionNotSavedError extends Error {
  readonly saved = false;
  readonly cause: unknown;

  constructor(cause: unknown) {
    super("学习动作没有保存");
    this.name = "LearningActionNotSavedError";
    this.cause = cause;
    initError(this, new.target.prototype);
  }
}

export class PullPageNotSavedError extends Error {
  readonly saved = false;
  readonly cause: unknown;

  constructor(cause: unknown) {
    super("同步页没有保存，游标未推进");
    this.name = "PullPageNotSavedError";
    this.cause = cause;
    initError(this, new.target.prototype);
  }
}

export class LessonNotFoundError extends Error {
  constructor() {
    super("本地没有这节课");
    this.name = "LessonNotFoundError";
    initError(this, new.target.prototype);
  }
}

export class OutboxEventNotFoundError extends Error {
  readonly saved = false;

  constructor() {
    super("outbox 里没有这个 eventId，没有改写其他事件");
    this.name = "OutboxEventNotFoundError";
    initError(this, new.target.prototype);
  }
}

export class AckConflictError extends Error {
  readonly saved = false;

  constructor() {
    super("这条 eventId 的确认结果与已保存的不一致，没有改写");
    this.name = "AckConflictError";
    initError(this, new.target.prototype);
  }
}

export class StorageCorruptError extends Error {
  constructor(store: string) {
    super(`本地数据无法读取：${store}`);
    this.name = "StorageCorruptError";
    initError(this, new.target.prototype);
  }
}

export class StorageReadError extends Error {
  readonly cause: unknown;

  constructor(cause: unknown) {
    super("本地队列没有读到，不能当作空队列");
    this.name = "StorageReadError";
    this.cause = cause;
    initError(this, new.target.prototype);
  }
}

export class StorageUnavailableError extends Error {
  readonly saved = false;

  constructor() {
    super("当前环境没有 IndexedDB 或 Android SQLite，没有保存");
    this.name = "StorageUnavailableError";
    initError(this, new.target.prototype);
  }
}

export class InjectedWriteFaultError extends Error {
  constructor() {
    super("注入的写入失败");
    this.name = "InjectedWriteFaultError";
    initError(this, new.target.prototype);
  }
}
