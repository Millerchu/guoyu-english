import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { feedbackForSaveFailure } from "../src/repositories/save-feedback.ts";
import {
  createUnimplementedLearningRepository,
  RepositoryNotImplementedError,
} from "../src/repositories/unimplemented.ts";

const homePageUrl = new URL("../src/pages/index/index.vue", import.meta.url);
const tokensUrl = new URL("../src/styles/tokens.css", import.meta.url);

test("unimplemented save rejects and is not presented as saved", async () => {
  const repository = createUnimplementedLearningRepository();
  await assert.rejects(
    () => repository.saveLearningAction({ eventId: "evt-1", stepId: "step-listen" }),
    (error: unknown) => {
      assert.ok(error instanceof RepositoryNotImplementedError);
      assert.equal(error.saved, false);
      const feedback = feedbackForSaveFailure(error);
      assert.equal(feedback.saved, false);
      assert.equal(feedback.message.includes("已保存"), false);
      return true;
    },
  );
});

test("other repository methods reject instead of reporting an empty success", async () => {
  const repository = createUnimplementedLearningRepository();
  await assert.rejects(() => repository.listOutbox(10), RepositoryNotImplementedError);
  await assert.rejects(
    () => repository.ackOutbox("evt-1", "ACCEPTED"),
    RepositoryNotImplementedError,
  );
  await assert.rejects(
    () => repository.savePullPage([], "cursor-1"),
    RepositoryNotImplementedError,
  );
  await assert.rejects(
    () => repository.getLesson("animals-001", 1),
    RepositoryNotImplementedError,
  );
});

test("homepage shell has one start button, no save claim, and no audio", () => {
  const page = readFileSync(homePageUrl, "utf8");
  const buttons = page.match(/<button\b/g) ?? [];
  assert.equal(buttons.length, 1);
  assert.equal(page.includes("开始学习"), true);
  assert.equal(page.includes("已保存"), false);
  assert.equal(page.includes("<audio"), false);
  assert.equal(page.includes("createInnerAudioContext"), false);
  assert.equal(page.includes("autoplay"), false);

  const tokens = readFileSync(tokensUrl, "utf8");
  const match = tokens.match(/--gy-button-height:\s*(\d+)px/);
  assert.ok(match);
  const height = Number(match[1]);
  assert.ok(height >= 56 && height <= 64);
});
