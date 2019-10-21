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
  <br-meta :meta="component.getMeta()">
    <slot>
      <br-node-container-item v-if="isContainerItem(component)" />

      <div v-else-if="isContainer(component)">
        <br-node-component v-for="(component, key) in component.getChildren()" :key="key" :component="component" />
      </div>

      <br-node-component v-else v-for="(component, key) in component.getChildren()" :key="key" :component="component" />
    </slot>
  </br-meta>
</template>

<script lang="ts">
import { Component as SpaComponent, Page, isContainerItem, isContainer } from '@bloomreach/spa-sdk';
import { Component, InjectReactive, Prop, Provide, Vue } from 'vue-property-decorator';
import BrMeta from './BrMeta.vue';
import BrNodeContainerItem from './BrNodeContainerItem.vue';

@Component({
  components: {
    BrMeta,
    BrNodeContainerItem,
  },
  methods: {
    isContainerItem,
    isContainer,
  },
})
export default class BrNodeComponent extends Vue {
  @Prop() component!: SpaComponent;

  @InjectReactive() private page!: Page;

  @Provide() // ProvideReactive doesn't work with recursive components
  private component$() {
    return this.component;
  }
}
</script>
