/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { VueConstructor } from 'vue';
import BrComponent from './BrComponent.vue';
import BrManageContentButton from './BrManageContentButton.vue';
import BrManageMenuButton from './BrManageMenuButton.vue';
import BrPage from './BrPage.vue';

/**
 * The brXM SDK plugin.
 */
export function BrSdk(vue: VueConstructor): void {
  vue.component('br-component', BrComponent);
  vue.component('br-manage-content-button', BrManageContentButton);
  vue.component('br-manage-menu-button', BrManageMenuButton);
  vue.component('br-page', BrPage);
}

export { BrComponent, BrManageContentButton, BrManageMenuButton, BrPage };
