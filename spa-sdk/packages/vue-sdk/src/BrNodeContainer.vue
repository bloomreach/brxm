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
  <div v-if="component.getType() === TYPE_CONTAINER_INLINE">
    <slot />
  </div>

  <div v-else-if="component.getType() === TYPE_CONTAINER_NO_MARKUP">
    <slot />
  </div>

  <div v-else-if="component.getType() === TYPE_CONTAINER_ORDERED_LIST">
    <slot />
  </div>

  <div v-else-if="component.getType() === TYPE_CONTAINER_UNORDERED_LIST">
    <slot />
  </div>

  <div v-else>
    <slot />
  </div>
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
import { Component, InjectReactive, Inject, Vue } from 'vue-property-decorator';

@Component({
  computed: {
    component(this: BrNodeContainer) {
      return this.component$();
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

  private component!: Container;

  @InjectReactive() private page!: Page;
}
</script>
