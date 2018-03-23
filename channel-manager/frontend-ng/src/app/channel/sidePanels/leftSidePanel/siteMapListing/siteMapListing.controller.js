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

const FILTERED_FIELDS = ['pageTitle', 'name', 'pathInfo'];

class SiteMapListingController {
  constructor($filter, $translate, HippoIframeService) {
    'ngInject';

    this.$filter = $filter;
    this.$translate = $translate;
    this.HippoIframeService = HippoIframeService;

    this.keywords = '';
  }

  $onChanges() {
    this.filteredItems = this.items;
  }

  filterItems() {
    this.filteredItems = this.$filter('search')(this.items, this.keywords, FILTERED_FIELDS);
  }

  clearFilter() {
    this.keywords = '';
    this.filteredItems = this.items;
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
