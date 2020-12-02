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
  <div v-if="document" :class="{ 'has-edit-button': page.isPreview() }">
    <br-manage-content-button :content="document" />
    <img v-if="image" class="img-fluid mb-3" :src="image.getOriginal().getUrl()" :alt="data.title" />
    <h1 v-if="data.title">{{ data.title }}</h1>
    <p v-if="data.author" class="mb-3 text-muted">{{ data.author }}</p>
    <p v-if="date" class="mb-3 small text-muted">{{ formatDate(date) }}</p>
    <div v-if="data.content" v-html="page.rewriteLinks(data.content.value)" />
  </div>
</template>

<script lang="ts">
import { ContainerItem, Document, ImageSet, Page } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'nuxt-property-decorator';

@Component({
  computed: {
    data(this: BrContent) {
      return this.document?.getData<DocumentData>();
    },

    document(this: BrContent) {
      const { document } = this.component.getModels<DocumentModels>();

      return document && this.page.getContent<Document>(document);
    },

    image(this: BrContent) {
      return this.data?.image && this.page.getContent<ImageSet>(this.data.image);
    },

    date(this: BrContent) {
      return this.data?.date ?? this.data?.publicationDate;
    },
  },
  methods: {
    formatDate(date: number) {
      return new Date(date).toDateString();
    },
  },
  name: 'br-content',
})
export default class BrContent extends Vue {
  @Prop() component!: ContainerItem;

  @Prop() page!: Page;

  data?: DocumentData;

  date?: number;

  document?: Document;

  image?: ImageSet;
}
</script>
