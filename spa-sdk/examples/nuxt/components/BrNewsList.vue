<!--
  Copyright 2020 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->

<template>
  <div v-if="pageable">
    <br-news-list-item v-for="(item, key) in pageable.items" :key="key" :item="page.getContent(item)" :page="page" />
    <div v-if="page.isPreview()" class="has-edit-button float-right">
      <br-manage-content-button
        document-template-query="new-news-document"
        folder-template-query="new-news-folder"
        root="news"
      />
    </div>
    <br-news-list-pagination v-if="pageable.showPagination" :pageable="pageable" :page="page" />
  </div>
</template>

<script lang="ts">
import { ContainerItem, Page } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'nuxt-property-decorator';
import BrNewsListItem from './BrNewsListItem.vue';
import BrNewsListPagination from './BrNewsListPagination.vue';

@Component({
  components: {
    BrNewsListItem,
    BrNewsListPagination,
  },
  computed: {
    pageable(this: BrNewsList) {
      const { pageable } = this.component.getModels<PageableModels>();

      return pageable;
    },
  },
  name: 'br-news-list',
})
export default class BrNewsList extends Vue {
  @Prop() component!: ContainerItem;

  @Prop() page!: Page;
}
</script>
