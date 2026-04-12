<template>
  <router-view />
</template>

<script setup>
</script>

<style>
:root {
  --app-primary: #3b82f6;
  --app-primary-hover: #2563eb;
  --app-primary-active: #1d4ed8;
  --app-bg: #f6f8fb;
  --app-card: #ffffff;
  --app-title: #111827;
  --app-text: #374151;
  --app-muted: #6b7280;
  --app-border: #e5e7eb;
  --app-success: #10b981;
  --app-warning: #f59e0b;
  --app-danger: #ef4444;
  --app-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
  background: var(--app-bg);
  color: var(--app-text);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
  line-height: 1.5;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

button,
input,
select,
textarea {
  font: inherit;
}
</style>
