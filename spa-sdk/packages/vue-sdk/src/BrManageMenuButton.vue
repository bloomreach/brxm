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
  <br-meta v-if="page && page.isPreview()" :meta="page.getMeta(menu._meta)" />
</template>

<script lang="ts">
import { Menu, Page } from '@bloomreach/spa-sdk';
import { Component, Inject, Prop, Vue } from 'vue-property-decorator';
import BrMeta from './BrMeta.vue';

/**
 * The button component that opens a menu editor.
 */
@Component({
  components: { BrMeta },
  computed: {
    page(this: BrManageMenuButton) {
      return this.page$?.();
    },
  },
  name: 'br-manage-menu-button',
})
export default class BrManageMenuButton extends Vue {
  /**
   * The related menu model.
   */
  @Prop() menu!: Menu;

  @Inject() private page$?: () => Page;
}
</script>
