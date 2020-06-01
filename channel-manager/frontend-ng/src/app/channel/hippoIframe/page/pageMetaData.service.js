/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import HstConstants from '../hst.constants';

class PageMetaDataService {
  constructor() {
    'ngInject';

    this.clear();
  }

  clear() {
    this.data = {};
  }

  // There can be multiple contributions to the page meta-data. Therefore, we "accumulate" the data here.
  add(data) {
    Object.assign(this.data, data);
  }

  get() {
    return this.data;
  }

  getPathInfo() {
    return this.data[HstConstants.PATH_INFO];
  }

  getChannelId() {
    return this.data[HstConstants.CHANNEL_ID];
  }

  getContextPath() {
    return this.data[HstConstants.CONTEXT_PATH];
  }

  getPageId() {
    return this.data[HstConstants.PAGE_ID];
  }

  getRenderVariant() {
    return this.data[HstConstants.RENDER_VARIANT];
  }

  getSiteMapItemId() {
    return this.data[HstConstants.SITEMAPITEM_ID];
  }
}

export default PageMetaDataService;
