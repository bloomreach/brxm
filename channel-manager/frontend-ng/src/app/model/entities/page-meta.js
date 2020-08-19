/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import * as HstConstants from '../constants';
import { Entity } from './entity';

export class PageMeta extends Entity {
  addMeta(meta) {
    Object.assign(this._meta, meta._meta);

    return this;
  }

  getPathInfo() {
    return this._meta[HstConstants.PATH_INFO];
  }

  getChannelId() {
    return this._meta[HstConstants.CHANNEL_ID];
  }

  getContextPath() {
    return this._meta[HstConstants.CONTEXT_PATH];
  }

  getPageId() {
    return this._meta[HstConstants.PAGE_ID];
  }

  getRenderVariant() {
    return this._meta[HstConstants.RENDER_VARIANT];
  }

  getSiteMapItemId() {
    return this._meta[HstConstants.SITEMAPITEM_ID];
  }

  isXPage() {
    return this._meta[HstConstants.EXPERIENCE_PAGE] === 'true';
  }

  getBranchId() {
    return this._meta[HstConstants.BRANCH_ID];
  }

  getUnpublishedVariantId() {
    return this._meta[HstConstants.UNPUBLISHED_VARIANT_ID];
  }
}
