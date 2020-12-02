/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Menu from './models/menu.model';
import MenuAction from './models/menuAction.model';
import MenuDivider from './models/menuDivider.model';

class MenuService {
  constructor() {
    'ngInject';

    this.dividerCount = 0;
  }

  defineMenu(name, config) {
    this.menu = new Menu(name, config);
    return this;
  }

  addAction(name, config) {
    this.menu.add(new MenuAction(name, config));
    return this;
  }

  findAction(name) {
    return this.menu.find(name);
  }

  addDivider(config) {
    this.menu.add(new MenuDivider(`divider-${this.dividerCount}`, config));
    this.dividerCount += 1;
    return this;
  }

  getMenu(showSubpageHandler) {
    this.showSubpageHandler = showSubpageHandler;
    return this.menu;
  }

  showSubPage(subpage) {
    if (this.showSubpageHandler) {
      this.showSubpageHandler(subpage);
    }
  }
}

export default MenuService;
