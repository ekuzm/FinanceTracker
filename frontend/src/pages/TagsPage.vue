<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Tags workspace"
      description="Create new tags and open any row to rename or remove it."
    >
      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Tag name" />
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="filters.search = ''">Reset</button>
        <button class="button button--primary" type="button" @click="openCreateModal">Add tag</button>
      </template>
    </FiltersToolbar>

    <section class="panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">Tag list</p>
          <h3>Tags</h3>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th class="table-amount">ID</th>
            </tr>
          </thead>
          <tbody v-if="filteredTags.length">
            <tr
              v-for="tag in filteredTags"
              :key="tag.id"
              class="clickable-row"
              @click="selectTag(tag)"
            >
              <td>{{ tag.name }}</td>
              <td class="table-amount">#{{ tag.id }}</td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="2" class="table-empty">No tags match the current search.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>

  <BaseDrawer
    v-model="tagDrawerOpen"
    :title="selectedTag?.name ?? 'Tag'"
    description="Open any tag to review it and choose the next action."
  >
    <div v-if="selectedTag" class="detail-grid">
      <div class="detail-row">
        <span>Name</span>
        <strong>{{ selectedTag.name }}</strong>
      </div>
      <div class="detail-row">
        <span>Tag ID</span>
        <strong>#{{ selectedTag.id }}</strong>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Actions</span>
        <div class="drawer-actions">
          <button class="button button--ghost" type="button" @click="handleEditFromDrawer">
            Edit
          </button>
          <button class="button button--danger" type="button" @click="handleDeleteFromDrawer">
            Delete
          </button>
        </div>
      </div>
    </div>
  </BaseDrawer>

  <BaseModal
    v-model="tagModalOpen"
    :title="editingTag ? 'Edit tag' : 'Add tag'"
    description="Keep tag naming short and consistent across the tracker."
  >
    <form class="form-grid" @submit.prevent="submitTag">
      <label class="input-shell input-shell--wide">
        <span>Name</span>
        <input v-model="tagForm.name" type="text" minlength="1" maxlength="50" required />
      </label>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="tagModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">
          {{ editingTag ? 'Save tag' : 'Create tag' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import BaseDrawer from '@/components/BaseDrawer.vue';
import BaseModal from '@/components/BaseModal.vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { useRouteQueryState } from '@/composables/useRouteQueryState';
import type { TagRequest, TagResponse } from '@/types/api';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';

const { tags, loadTags, createTag, updateTag, deleteTag } = useFinanceTracker();
const { confirm } = useConfirmDialog();

const filters = useRouteQueryState({
  search: '',
});

const tagDrawerOpen = ref(false);
const tagModalOpen = ref(false);
const editingTag = ref<TagResponse | null>(null);
const selectedTag = ref<TagResponse | null>(null);

const tagForm = reactive<TagRequest>({
  name: '',
});

onMounted(() => {
  void loadTags();
});

const filteredTags = computed(() => {
  const search = filters.search.trim().toLowerCase();
  return tags.value.filter((tag) => !search || tag.name.toLowerCase().includes(search));
});

function resetTagForm() {
  tagForm.name = '';
}

function openCreateModal() {
  editingTag.value = null;
  resetTagForm();
  tagModalOpen.value = true;
}

function openEditModal(tag: TagResponse) {
  editingTag.value = tag;
  tagForm.name = tag.name;
  tagModalOpen.value = true;
}

function selectTag(tag: TagResponse) {
  selectedTag.value = tag;
  tagDrawerOpen.value = true;
}

async function submitTag() {
  const payload = { name: tagForm.name.trim() };
  if (!payload.name) {
    return;
  }

  if (editingTag.value) {
    await updateTag(editingTag.value.id, payload);
  } else {
    await createTag(payload);
  }

  tagModalOpen.value = false;
  await loadTags();
}

async function handleDelete(tag: TagResponse) {
  const confirmed = await confirm({
    title: `Delete tag "${tag.name}"?`,
    description: 'This tag will be removed from the workspace and detached from future selection.',
    confirmLabel: 'Delete',
  });

  if (!confirmed) {
    return;
  }

  await deleteTag(tag.id);
  if (selectedTag.value?.id === tag.id) {
    tagDrawerOpen.value = false;
    selectedTag.value = null;
  }
  await loadTags();
}

function handleEditFromDrawer() {
  if (!selectedTag.value) {
    return;
  }

  tagDrawerOpen.value = false;
  openEditModal(selectedTag.value);
}

async function handleDeleteFromDrawer() {
  if (!selectedTag.value) {
    return;
  }

  tagDrawerOpen.value = false;
  await handleDelete(selectedTag.value);
}
</script>
