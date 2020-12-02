
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

import MenuItem from './menuItem.model';

class Menu extends MenuItem {
  constructor(name, config = {}) {
    super(name, config);
    this.items = [];
    this.width = config.width || 3;
  }

  add(item) {
    this.items.push(item);
  }

  find(name) {
    return this.items.find(item => item.name === name);
  }

  hasIcons() {
    return this.items
      .filter(item => item.type === 'action')
      .some(item => item.hasIconSvg() || item.hasIconName());
  }
}

export default Menu;
