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
  <ul v-if="menu" class="navbar-nav col-12" :class="{ 'has-edit-button': page.isPreview() }">
    <br-manage-menu-button :menu="menu" />
    <li v-for="(item, index) in menu.getItems()" :key="index" class="nav-item" :class="{ active: item.isSelected() }">
      <span v-if="!item.getUrl()" class="nav-link text-capitalize disabled">
        {{ item.getName() }}
      </span>

      <a v-else-if="item.getLink().type === TYPE_LINK_EXTERNAL" class="nav-link text-capitalize" :href="item.getUrl()">
        {{ item.getName() }}
      </a>

      <router-link v-else :to="item.getUrl()" class="nav-link text-capitalize">
        {{ item.getName() }}
      </router-link>
    </li>
  </ul>
</template>

<script lang="ts">
import { Component as BrComponent, Menu, Page, TYPE_LINK_EXTERNAL, isMenu } from '@bloomreach/spa-sdk';
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component({
  computed: {
    menu(this: BrMenu) {
      const menuRef = this.component.getModels<MenuModels>()?.menu;
      const menu = menuRef && this.page.getContent<Menu>(menuRef);

      return isMenu(menu) ? menu : undefined;
    },
  },
  data: () => ({ TYPE_LINK_EXTERNAL }),
  name: 'br-menu',
})
export default class BrMenu extends Vue {
  @Prop() component!: BrComponent;

  @Prop() page!: Page;
}
</script>
