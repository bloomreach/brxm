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

import { Subject } from 'rxjs';

class PageService {
  constructor($q, $rootScope, $state, HippoIframeService, HstService, PageStructureService, SiteMapService) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$state = $state;
    this.HippoIframeService = HippoIframeService;
    this.HstService = HstService;
    this.PageStructureService = PageStructureService;
    this.SiteMapService = SiteMapService;

    this.actions = null;
    this.states = null;
    this.states$ = new Subject();

    this.$rootScope.$on('page:change', async () => {
      await this.load();

      if (this.$state.$current.name.startsWith('hippo-cm.channel.edit-page')) {
        this.syncPageEditor();
      }
    });
    this.$rootScope.$on('page:check-changes', () => this.load());
  }

  get isXPage() {
    return 'xpage' in this.states;
  }

  get xPageId() {
    return this.states && this.states.xpage ? this.states.xpage.id : undefined;
  }

  async load() {
    const page = this.PageStructureService.getPage();
    if (!page) {
      this.actions = null;
      this.states = null;

      return;
    }

    const meta = page.getMeta();

    try {
      const { data: { actions, states } } = await this.HstService.doGet(
        `${meta.getPageId()}`,
        'item',
        `${meta.getSiteMapItemId()}`,
      );

      this.actions = actions;
      this.states = states;
      this.states$.next(states);
    } catch (e) {
      this.actions = null;
      this.states = null;
      this.states$.next(undefined);
    }
  }

  hasActions(category) {
    return !!(this.actions && this.actions[category]);
  }

  hasAction(category, name) {
    return this.getAction(category, name) !== null;
  }

  hasSomeAction(category, ...names) {
    if (!this.hasActions(category)) {
      return false;
    }

    const { items } = this.actions[category];
    return names.some(name => items[name]);
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

  hasState(category) {
    return !!(this.states && this.states[category]);
  }

  getState(category) {
    return this.hasState(category)
      ? this.states[category]
      : null;
  }

  syncPageEditor() {
    if (this.isXPage) {
      this.$state.go('hippo-cm.channel.edit-page.content', { documentId: this.xPageId });
      return;
    }

    function findSiteMapItem(queue, val) {
      while (queue.length > 0) {
        const currentObj = queue.shift();
        if (currentObj.renderPathInfo === val) {
          return currentObj;
        }

        queue.push(...currentObj.children);
      }

      return false;
    }

    const sitemap = this.SiteMapService.get();
    const currentSitemapItem = findSiteMapItem([...sitemap], this.HippoIframeService.getCurrentRenderPathInfo());
    if (currentSitemapItem) {
      const title = currentSitemapItem.pageTitle || currentSitemapItem.name;
      this.$state.go('hippo-cm.channel.edit-page-unavailable', { title });
    }
  }
}

export default PageService;
