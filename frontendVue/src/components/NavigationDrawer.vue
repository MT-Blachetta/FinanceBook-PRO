<!--
  Slide-out navigation drawer – ported from the inline React NavigationDrawer.
-->
<script setup lang="ts">
defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
  navigate: [path: string]
}>()
</script>

<template>
  <div class="drawer" :class="{ open }">
    <h3>Menu</h3>
    <ul>
      <li><button @click="emit('navigate', '/')">Summary</button></li>
      <li><button @click="emit('navigate', '/add')">Add Payment</button></li>
      <li><button @click="emit('navigate', '/categories')">Categories</button></li>
      <li><button @click="emit('navigate', '/category-types')">Category Types</button></li>
      <li><button @click="emit('navigate', '/statistics')">Statistics</button></li>
    </ul>
  </div>
  <div v-if="open" class="overlay" @click="emit('close')" />
</template>

<style scoped>
.drawer {
  position: fixed;
  top: 0;
  left: -300px;
  width: 300px;
  height: 100%;
  background: #222;
  color: white;
  padding: 20px;
  transition: left 0.3s ease;
  z-index: 1000;
  border-right: 1px solid #444;
}

.drawer.open {
  left: 0;
}

.drawer h3 {
  margin-bottom: 1rem;
}

.drawer ul {
  list-style: none;
  padding: 0;
}

.drawer li {
  margin-bottom: 10px;
}

.drawer button {
  background: none;
  border: none;
  color: var(--color-text-primary);
  cursor: pointer;
  font-size: 1rem;
  padding: 0.5rem 0;
  width: 100%;
  text-align: left;
}

.drawer button:hover {
  color: var(--color-positive);
}

.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
}
</style>
