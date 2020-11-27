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
  <div v-if="document" class="jumbotron mb-3" :class="{ 'has-edit-button': page.isPreview() }">
    <br-manage-content-button
      :content="document"
      document-template-query="new-banner-document"
      folder-template-query="new-banner-folder"
      parameter="document"
      root="banners"
      :relative="true"
    />
    <h1 v-if="data.title">{{ data.title }}</h1>
    <img v-if="image" class="img-fluid" :src="image.getOriginal().getUrl()" :alt="data.title" />
    <div v-if="data.content" v-html="page.rewriteLinks(data.content.value)" />
    <p v-if="link" className="lead">
      <nuxt-link :to="link.getUrl()" class="btn btn-primary btn-lg" role="button">Learn more</nuxt-link>
    </p>
  </div>
</template>

<script lang="ts">
import { ContainerItem, Document, ImageSet, Page } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'nuxt-property-decorator';

@Component({
  computed: {
    data(this: BrBanner) {
      return this.document?.getData<DocumentData>();
    },

    document(this: BrBanner) {
      const { document } = this.component.getModels<DocumentModels>();

      return document && this.page.getContent<Document>(document);
    },

    image(this: BrBanner) {
      return this.data?.image && this.page.getContent<ImageSet>(this.data.image);
    },

    link(this: BrBanner) {
      return this.data?.link && this.page.getContent<Document>(this.data.link);
    },
  },
  name: 'br-banner',
})
export default class BrBanner extends Vue {
  @Prop() component!: ContainerItem;

  @Prop() page!: Page;

  data?: DocumentData;

  document?: Document;

  image?: ImageSet;

  link?: Document;
}
</script>
