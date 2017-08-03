/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

class MenuItem {

  constructor(name, config = {}) {
    this.name = name;
    this.translationKey = config.translationKey || name;
    this.translationKeyFunction = config.translationKeyFunction;

    this.isVisible = config.isVisible || this.isVisible;
    this.isEnabled = config.isEnabled || this.isEnabled;
    this.onClick = config.onClick || this.onClick;

    this.iconSvg = config.iconSvg || undefined;
    this.iconName = config.iconName || undefined;
    this.isIconVisible = config.isIconVisible || this.isIconVisible;
  }

  isVisible() {
    return true;
  }

  isEnabled() {
    return true;
  }

  onClick() {}

  hasIconName() {
    return this.iconName !== undefined && this.iconName !== null;
  }

  hasIconSvg() {
    return this.iconSvg !== undefined && this.iconSvg !== null;
  }

  isIconVisible() {
    return this.hasIconName() || this.hasIconSvg();
  }
}

export default MenuItem;

