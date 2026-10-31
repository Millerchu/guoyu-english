import type { LocalDatabase } from "./local-database";
import { lessonKeyOf } from "./record-parse";
import type { LearningStepId } from "./types";

export const ANIMALS_LESSON_ID = "animals-001";
export const ANIMALS_LESSON_VERSION = 1;

export const ANIMALS_LESSON_STEPS: readonly LearningStepId[] = [
  "step-listen",
  "step-speak",
  "step-recognize",
  "step-read",
];

export async function seedContractLessons(db: LocalDatabase): Promise<void> {
  const lessonKey = lessonKeyOf(ANIMALS_LESSON_ID, ANIMALS_LESSON_VERSION);
  await db.transaction("readwrite", async (tx) => {
    const existing = await tx.get("lesson", lessonKey);
    if (existing) {
      return;
    }
    await tx.put("lesson", {
      lessonKey,
      lessonId: ANIMALS_LESSON_ID,
      version: ANIMALS_LESSON_VERSION,
      stepsJson: JSON.stringify(ANIMALS_LESSON_STEPS),
    });
  });
}
