/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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

import { Link } from './link';
import { MetaCollectionModel } from './meta-collection';

type MenuItemLinks = 'site';

/**
 * Essentials component menu model.
 */
export interface Menu {
  _meta?: MetaCollectionModel;

  /**
   * @deprecated The parameter was removed in the Experience Manager 14.2.
   */
  selectSiteMenuItem?: MenuItem | null;

  siteMenuItems: MenuItem[];
}

/**
 * Essentials component menu item model.
 */
export interface MenuItem {
  childMenuItems: MenuItem[];
  depth: number;
  expanded: boolean;
  name: string;
  parameters: object;
  repositoryBased: boolean;
  selected: boolean;
  _links: Partial<Record<MenuItemLinks, Link>>;
}
