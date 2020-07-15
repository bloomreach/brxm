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

class PageService {
  constructor($q, $rootScope, HstService, PageStructureService) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;

    this.$rootScope.$on('page:change', this._onPageChange.bind(this));
  }

  _onPageChange() {
    const page = this.PageStructureService.getPage();
    if (!page) {
      this.actions = null;
      this.states = null;

      return;
    }

    const meta = page.getMeta();
    this.HstService.doGet(`${meta.getPageId()}`, 'item', `${meta.getSiteMapItemId()}`)
      .then(({ data: { actions, states } }) => {
        this.actions = actions;
        this.states = states;
      })
      .catch(() => {
        this.actions = null;
        this.states = null;
      });
  }

  hasActions(category) {
    return !!(this.actions && this.actions[category]);
  }

  hasAction(category, name) {
    return this.getAction(category, name) !== null;
  }

  getAction(category, name) {
    if (!this.hasActions(category)) {
      return null;
    }

    return (this.actions[category].items && this.actions[category].items[name]) || null;
  }

  isActionEnabled(category, name) {
    const action = this.getAction(category, name);
    return !!(action && action.enabled);
  }
}

export default PageService;
