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
  <br-meta v-if="page && page.isPreview()" :meta="meta" />
</template>

<script lang="ts">
import { ManageContentButton, Page, TYPE_MANAGE_CONTENT_BUTTON } from '@bloomreach/spa-sdk';
import { Component, Inject, Prop, Vue } from 'vue-property-decorator';
import BrMeta from './BrMeta.vue';

/**
 * The button component that opens for editing a content.
 */
@Component({
  components: { BrMeta },
  computed: {
    meta(this: BrManageContentButton) {
      return this.page?.getButton(TYPE_MANAGE_CONTENT_BUTTON, this.$props);
    },

    page(this: BrManageContentButton) {
      return this.page$?.();
    },
  },
  name: 'br-manage-content-button',
})
export default class BrManageContentButton extends Vue implements ManageContentButton {
  @Prop() content?: ManageContentButton['content'];

  @Prop() documentTemplateQuery?: ManageContentButton['documentTemplateQuery'];

  @Prop() folderTemplateQuery?: ManageContentButton['folderTemplateQuery'];

  @Prop() path?: ManageContentButton['path'];

  @Prop() parameter?: ManageContentButton['parameter'];

  @Prop() relative?: ManageContentButton['relative'];

  @Prop() root?: ManageContentButton['root'];

  page?: Page;

  @Inject() private page$?: () => Page;
}
</script>
