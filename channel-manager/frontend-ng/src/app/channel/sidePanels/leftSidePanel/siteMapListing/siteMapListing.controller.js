/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class SiteMapListingController {
  constructor($filter, HippoIframeService) {
    'ngInject';

    this.HippoIframeService = HippoIframeService;

    const startWithSlashFilter = $filter('startWithSlash');
    this.filteredFields = ['pageTitle', 'name', item => startWithSlashFilter(item.pathInfo)];
    this.filteredItems = [];
  }

  onFilter(filteredItems) {
    this.filteredItems = filteredItems;
  }

  showPage(siteMapItem) {
    this.HippoIframeService.load(siteMapItem.renderPathInfo);
  }

  isActiveSiteMapItem(siteMapItem) {
    return siteMapItem.renderPathInfo === this.HippoIframeService.getCurrentRenderPathInfo();
  }

  get activeItemIndex() {
    return this.filteredItems.findIndex(item => this.isActiveSiteMapItem(item));
  }

  set activeItemIndex(index) {
    // ignore update by md-virtual-repeat-container on scroll
  }
}

export default SiteMapListingController;
