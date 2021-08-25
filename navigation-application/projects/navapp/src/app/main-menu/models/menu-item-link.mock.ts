/*!
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

import { NavItemMock } from '../../models/dto/nav-item-dto.mock';

import { MenuItemLink } from './menu-item-link.model';

export class MenuItemLinkMock extends MenuItemLink {
  constructor({
    caption = 'testCaption',
    icon = 'testIcon',
    id = 'testId',
    navItem = new NavItemMock({
      id: 'some-id',
      appIframeUrl: 'some/app/url',
      appPath: 'app/path',
    }),
  } = {}) {
    super(id, caption, icon);

    this.navItem = navItem;
  }
}
