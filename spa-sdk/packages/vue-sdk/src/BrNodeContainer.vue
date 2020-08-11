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
  <component
    v-if="component.getType() in mapping"
    :is="mapping[component.getType()]"
    :component="component"
    :page="page"
  >
    <slot />
  </component>

  <br-container-inline v-else-if="component.getType() === TYPE_CONTAINER_INLINE" :component="component" :page="page">
    <slot />
  </br-container-inline>

  <br-container-no-markup
    v-else-if="component.getType() === TYPE_CONTAINER_NO_MARKUP"
    :component="component"
    :page="page"
  >
    <slot />
  </br-container-no-markup>

  <br-container-ordered-list
    v-else-if="component.getType() === TYPE_CONTAINER_ORDERED_LIST"
    :component="component"
    :page="page"
  >
    <slot />
  </br-container-ordered-list>

  <br-container-unordered-list
    v-else-if="component.getType() === TYPE_CONTAINER_UNORDERED_LIST"
    :component="component"
    :page="page"
  >
    <slot />
  </br-container-unordered-list>

  <br-container-box v-else :component="component" :page="page">
    <slot />
  </br-container-box>
</template>

<script lang="ts">
import {
  Container,
  Page,
  TYPE_CONTAINER_INLINE,
  TYPE_CONTAINER_NO_MARKUP,
  TYPE_CONTAINER_ORDERED_LIST,
  TYPE_CONTAINER_UNORDERED_LIST,
} from '@bloomreach/spa-sdk';
import { Component, Inject, Vue } from 'vue-property-decorator';
import BrContainerBox from './BrContainerBox.vue';
import BrContainerInline from './BrContainerInline.vue';
import BrContainerNoMarkup from './BrContainerNoMarkup.vue';
import BrContainerOrderedList from './BrContainerOrderedList.vue';
import BrContainerUnorderedList from './BrContainerUnorderedList.vue';

@Component({
  components: {
    BrContainerBox,
    BrContainerInline,
    BrContainerNoMarkup,
    BrContainerOrderedList,
    BrContainerUnorderedList,
  },
  computed: {
    component(this: BrNodeContainer) {
      return this.component$();
    },
    mapping(this: BrNodeContainer) {
      return this.mapping$();
    },
    page(this: BrNodeContainer) {
      return this.page$();
    },
  },
  data: () => ({
    TYPE_CONTAINER_INLINE,
    TYPE_CONTAINER_NO_MARKUP,
    TYPE_CONTAINER_ORDERED_LIST,
    TYPE_CONTAINER_UNORDERED_LIST,
  }),
})
export default class BrNodeContainer extends Vue {
  @Inject() private component$!: () => Container;

  @Inject() private mapping$!: () => Record<string, Vue.Component>;

  @Inject() private page$!: () => Page;
}
</script>
