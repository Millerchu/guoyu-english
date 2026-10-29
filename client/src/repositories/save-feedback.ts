export interface SaveFailureFeedback {
  readonly saved: false;
  readonly message: string;
}

export function feedbackForSaveFailure(_error: unknown): SaveFailureFeedback {
  return {
    saved: false,
    message: "还没有保存",
  };
}
