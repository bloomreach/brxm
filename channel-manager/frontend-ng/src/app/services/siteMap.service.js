/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

class SiteMapService {
  constructor(HstService, FeedbackService) {
    'ngInject';

    this.HstService = HstService;
    this.FeedbackService = FeedbackService;

    this.siteMap = [];
  }

  get() {
    return this.siteMap;
  }

  load(siteMapId) {
    return this.HstService.getSiteMapTree(siteMapId)
      .then((siteMap) => {
        this.siteMap = [siteMap];
      })
      .catch(() => {
        this.siteMap = [];
        this.FeedbackService.showError('ERROR_SITEMAP_RETRIEVAL_FAILED');
      });
  }

  create(siteMapId, parentSiteMapItemId, page) {
    return this.HstService.doPost(page, siteMapId, 'create', parentSiteMapItemId)
      .then(response => response.data);
  }

  copy(siteMapId, headers) {
    return this.HstService.doPostWithHeaders(siteMapId, headers, 'copy')
      .then(response => response.data);
  }
}

export default SiteMapService;
