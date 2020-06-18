/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Injectable } from '@angular/core';

import { MenuItem } from '../main-menu/models/menu-item.model';

const prefix = 'qa';

@Injectable({
  providedIn: 'root',
})
export class QaHelperService {
  getMenuItemClass(item: MenuItem | string): string {
    if (typeof item === 'string') {
      return `${prefix}-menu-item-${item}`;
    }

    const classNamesList: string[] = [];

    classNamesList.push(`menu-item-${this.hyphenate(item.caption)}`);

    return classNamesList.map(className => `${prefix}-${className}`).join(' ');
  }

  private hyphenate(value: string): string {
    return value
      .toLowerCase()
      .split(' ')
      .join('-');
  }
}
