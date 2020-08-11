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

import { Component, HostBinding, Input } from '@angular/core';
import { Component as BrComponent, Menu, Page, isMenu } from '@bloomreach/spa-sdk';

@Component({
  selector: 'ul.br-menu', // tslint:disable-line: component-selector
  templateUrl: './menu.component.html',
})
export class MenuComponent {
  @Input() component!: BrComponent;
  @Input() page!: Page;

  @HostBinding('class') class = 'navbar-nav col-12';

  @HostBinding('class.has-edit-button')
  get isPreview() {
    return this.page.isPreview();
  }

  get menu() {
    const menuRef = this.component.getModels<MenuModels>()?.menu;
    const menu = menuRef && this.page.getContent<Menu>(menuRef);

    return isMenu(menu) ? menu : undefined;
  }
}
