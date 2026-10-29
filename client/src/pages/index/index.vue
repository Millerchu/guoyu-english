<template>
  <view class="home-screen">
    <view class="home-task">
      <text class="home-kicker">今日英语</text>
      <text class="home-title">听一听</text>
      <button class="home-start" @click="onStart">开始学习</button>
      <text v-if="hint.length > 0" class="home-hint">{{ hint }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const hint = ref("");

function onStart(): void {
  // 学习页还没有。这里不写进度，也不播放音频。
  hint.value = "听一听还没打开";
}
</script>

<style>
.home-screen {
  box-sizing: border-box;
  min-height: 100%;
  padding: 48px var(--gy-space-page) 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.home-task {
  width: 100%;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.home-kicker {
  color: var(--gy-color-muted);
  font-size: var(--gy-font-body);
  line-height: 1.4;
}

.home-title {
  margin-top: 8px;
  color: var(--gy-color-ink);
  font-size: var(--gy-font-title);
  font-weight: 700;
  line-height: 1.3;
}

.home-start {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 100%;
  height: var(--gy-button-height);
  min-height: 56px;
  margin: var(--gy-space-block) 0 0;
  padding: 0 24px;
  border: 0;
  border-radius: var(--gy-radius-button);
  background: linear-gradient(180deg, var(--gy-color-primary-soft) 0%, var(--gy-color-primary) 100%);
  box-shadow: var(--gy-shadow-button);
  color: var(--gy-color-on-primary);
  font-size: var(--gy-font-button);
  font-weight: 700;
  line-height: 1;
}

.home-start::after {
  border: none;
}

.home-hint {
  margin-top: 16px;
  color: var(--gy-color-muted);
  font-size: var(--gy-font-body);
  line-height: 1.4;
  text-align: center;
}
</style>
