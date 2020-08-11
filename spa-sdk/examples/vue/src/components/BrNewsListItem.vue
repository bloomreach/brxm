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
  <div class="card mb-3">
    <div class="card-body">
      <br-manage-content-button :content="item" />
      <h2 v-if="data.title" class="card-title">
        <router-link :to="item.getUrl()">{{ data.title }}</router-link>
      </h2>
      <div v-if="data.author" class="card-subtitle mb-3 text-muted">
        {{ data.author }}
      </div>
      <div v-if="data.date" class="card-subtitle mb-3 small text-muted">
        {{ formatDate(data.date) }}
      </div>
      <p v-if="data.introduction" class="card-text">
        {{ data.introduction }}
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { Document, Page } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  computed: {
    data(this: BrNewsListItem) {
      return this.item.getData<DocumentData>();
    },
  },
  methods: {
    formatDate(date: number) {
      return new Date(date).toDateString();
    },
  },
  name: 'br-news-list-item',
})
export default class BrNewsListItem extends Vue {
  @Prop() item!: Document;

  @Prop() page!: Page;
}
</script>
