<!--
  Category Types management – ported from React CategoryManagerPage.tsx.
-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { categoryTypeApi } from '@/api/category.api'
import ConfirmationDialog from '@/components/ConfirmationDialog.vue'
import type { CategoryType } from '@/types/models'

const router = useRouter()

const types = ref<CategoryType[]>([])
const isLoading = ref(true)
const newName = ref('')
const newDescription = ref('')
const deleteTarget = ref<CategoryType | null>(null)

onMounted(async () => {
  try {
    types.value = await categoryTypeApi.getAll()
  } catch (e) {
    console.error('Failed to load category types:', e)
  } finally {
    isLoading.value = false
  }
})

async function handleCreate() {
  if (!newName.value.trim()) return
  try {
    const created = await categoryTypeApi.create({
      name: newName.value.trim(),
      description: newDescription.value.trim() || undefined,
    })
    types.value.push(created)
    newName.value = ''
    newDescription.value = ''
  } catch (e) {
    console.error('Failed to create category type:', e)
    alert('Failed to create category type.')
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await categoryTypeApi.delete(deleteTarget.value.id)
    types.value = types.value.filter((t) => t.id !== deleteTarget.value!.id)
  } catch (e) {
    console.error('Failed to delete category type:', e)
    alert('Failed to delete category type.')
  } finally {
    deleteTarget.value = null
  }
}
</script>

<template>
  <div class="types-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← Back</button>
      <h2>Category Types</h2>
    </div>

    <div v-if="isLoading" class="status">Loading…</div>

    <template v-else>
      <div class="create-form">
        <input v-model="newName" type="text" placeholder="Type name…" />
        <input v-model="newDescription" type="text" placeholder="Description (optional)…" />
        <button class="btn-create" @click="handleCreate">Add</button>
      </div>

      <ul class="type-list">
        <li v-for="t in types" :key="t.id" class="type-item">
          <div class="type-info">
            <span class="type-name">{{ t.name }}</span>
            <span v-if="t.description" class="type-desc">{{ t.description }}</span>
          </div>
          <button class="btn-delete" @click="deleteTarget = t">×</button>
        </li>
      </ul>

      <div v-if="types.length === 0" class="status">
        No category types found. Create one above.
      </div>
    </template>

    <ConfirmationDialog
      :open="deleteTarget !== null"
      title="Delete Category Type"
      :message="`Delete type '${deleteTarget?.name}'? This may affect related categories.`"
      confirm-label="Delete"
      @confirm="confirmDelete"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<style scoped>
.types-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-header h2 { color: var(--color-text-primary); }

.back-btn {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.95rem;
}

.create-form {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid #272727;
}

.create-form input {
  flex: 1;
  min-width: 120px;
  background: #111;
  border: 1px solid #333;
  border-radius: var(--radius-md);
  padding: 0.5rem;
  color: var(--color-text-primary);
}

.btn-create {
  background: var(--color-positive);
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
}

.type-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid #272727;
}

.type-name { color: var(--color-text-primary); font-weight: 500; }
.type-desc { color: var(--color-text-secondary); font-size: 0.85rem; display: block; }

.btn-delete {
  background: var(--color-negative);
  color: white;
  border: none;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.status {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 2rem;
}
</style>
