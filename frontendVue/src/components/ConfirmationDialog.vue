<!--
  Confirmation dialog – ported from React ConfirmationDialog.tsx.
  Simple modal overlay with confirm/cancel buttons.
-->
<script setup lang="ts">
defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="overlay" @click.self="emit('cancel')">
      <div class="dialog">
        <h3>{{ title }}</h3>
        <p>{{ message }}</p>
        <div class="actions">
          <button class="btn-cancel" @click="emit('cancel')">
            {{ cancelLabel || 'Cancel' }}
          </button>
          <button class="btn-confirm" @click="emit('confirm')">
            {{ confirmLabel || 'Confirm' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog {
  background: var(--color-surface);
  border: 1px solid #333;
  border-radius: var(--radius-lg);
  padding: 1.5rem;
  max-width: 400px;
  width: 90%;
}

.dialog h3 {
  margin-bottom: 0.5rem;
  color: var(--color-text-primary);
}

.dialog p {
  color: var(--color-text-secondary);
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.btn-cancel {
  background: #333;
  color: var(--color-text-primary);
  border: 1px solid #444;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.btn-confirm {
  background: var(--color-negative);
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  cursor: pointer;
}
</style>
