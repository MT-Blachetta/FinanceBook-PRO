<!--
  Category management page – ported from React CategoryEditPage.tsx.
  Displays category tree with CRUD operations.
-->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi, categoryTypeApi } from '@/api/category.api'
import ConfirmationDialog from '@/components/ConfirmationDialog.vue'
import type { Category, CategoryType } from '@/types/models'
import { CATEGORY_NAME_MAX_LENGTH } from '@/utils/constants'

const router = useRouter()

const categories = ref<Category[]>([])
const categoryTypes = ref<CategoryType[]>([])
const isLoading = ref(true)
const newCategoryName = ref('')
const selectedTypeId = ref<number | null>(null)
const selectedParentId = ref<number | null>(null)
const deleteTarget = ref<Category | null>(null)

onMounted(async () => {
  try {
    const [cats, types] = await Promise.all([
      categoryApi.getAll(),
      categoryTypeApi.getAll(),
    ])
    categories.value = cats
    categoryTypes.value = types
    if (types.length > 0) selectedTypeId.value = types[0].id
  } catch (e) {
    console.error('Failed to load categories:', e)
  } finally {
    isLoading.value = false
  }
})

async function handleCreate() {
  if (!newCategoryName.value.trim() || !selectedTypeId.value) return
  try {
    const created = await categoryApi.create({
      name: newCategoryName.value.trim(),
      typeId: selectedTypeId.value,
      parentId: selectedParentId.value,
    })
    categories.value.push(created)
    newCategoryName.value = ''
  } catch (e) {
    console.error('Failed to create category:', e)
    alert('Failed to create category.')
  }
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  try {
    await categoryApi.delete(deleteTarget.value.id)
    categories.value = categories.value.filter((c) => c.id !== deleteTarget.value!.id)
  } catch (e) {
    console.error('Failed to delete category:', e)
    alert('Failed to delete category.')
  } finally {
    deleteTarget.value = null
  }
}
</script>

<template>
  <div class="categories-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← Back</button>
      <h2>Categories</h2>
    </div>

    <div v-if="isLoading" class="status">Loading…</div>

    <template v-else>
      <!-- Create form -->
      <div class="create-form">
        <input
          v-model="newCategoryName"
          type="text"
          :maxlength="CATEGORY_NAME_MAX_LENGTH"
          placeholder="New category name…"
        />
        <select v-model="selectedTypeId">
          <option v-for="t in categoryTypes" :key="t.id" :value="t.id">
            {{ t.name }}
          </option>
        </select>
        <select v-model="selectedParentId">
          <option :value="null">— No parent —</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>
        <button class="btn-create" @click="handleCreate">Add</button>
      </div>

      <!-- Category list -->
      <ul class="category-list">
        <li v-for="cat in categories" :key="cat.id" class="category-item">
          <div class="cat-info">
            <span class="cat-name">{{ cat.name }}</span>
            <span v-if="cat.parentId" class="cat-parent">(child)</span>
          </div>
          <button class="btn-delete" @click="deleteTarget = cat">×</button>
        </li>
      </ul>

      <div v-if="categories.length === 0" class="status">
        No categories found. Create one above.
      </div>
    </template>

    <ConfirmationDialog
      :open="deleteTarget !== null"
      title="Delete Category"
      :message="`Delete category '${deleteTarget?.name}'?`"
      confirm-label="Delete"
      @confirm="confirmDelete"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<style scoped>
.categories-page {
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

.create-form input,
.create-form select {
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

.category-list {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid #272727;
}

.cat-name { color: var(--color-text-primary); font-weight: 500; }
.cat-parent { color: var(--color-text-secondary); font-size: 0.8rem; margin-left: 0.5rem; }

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
