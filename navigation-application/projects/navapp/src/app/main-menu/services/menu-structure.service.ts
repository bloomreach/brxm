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

import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';

@Injectable()
export class MenuStructureService {
  private extensions = new MenuItemContainer('Extensions', [], 'extensions');

  addExtension(item: MenuItemLink): void {
    this.extensions.children.push(item);
  }

  getMenuStructure(): MenuItem[] {
    return this.createMenuStructure();
  }

  private createMenuStructure(): MenuItem[] {
    const home = new MenuItemLink(
      'hippo-perspective-dashboardperspective',
      'Home',
    );

    const experienceManager = new MenuItemLink(
      'hippo-perspective-channelmanagerperspective',
      'Experience manager',
      'experience-manager',
    );

    const projects = new MenuItemLink(
      'hippo-perspective-projectsperspective',
      'Projects',
      'projects',
    );

    const content = new MenuItemLink(
      'hippo-perspective-browserperspective',
      'Content',
      'documents',
    );

    const documentSearch = new MenuItemLink(
      'hippo-perspective-searchperspective',
      'Document search',
      'document-search',
    );

    const categories = new MenuItemContainer(
      'Categories',
      [
        new MenuItemLink('categories/category-ranking', 'Category ranking'),
        new MenuItemLink('categories/all-category-pages', 'All category pages'),
        new MenuItemLink('categories/category-ranking-diagnostics', 'Category ranking diagnostics'),
        new MenuItemLink('categories/category-facets-ranking', 'Category facets'),
        new MenuItemLink('categories/category-banners', 'Category banners'),
      ],
      'categories',
    );

    const siteSearch = new MenuItemContainer(
      'Site search',
      [
        new MenuItemLink('site-search/search-ranking', 'Search ranking'),
        new MenuItemLink('site-search/redirects', 'Redirects'),
        new MenuItemLink('site-search/search-ranking-diagnostics', 'Search ranking diagnostics'),
        new MenuItemLink('site-search/search-facets', 'Search facets'),
        new MenuItemLink('site-search/search-banners', 'Search banners'),
        new MenuItemLink('site-search/autosuggest-blacklist', 'Autosuggest blacklist'),
        new MenuItemLink('site-search/synonyms', 'Synonyms'),
      ],
      'site-search',
    );

    const seo = new MenuItemLink('seo', 'SEO', 'seo');
    const widgets = new MenuItemLink('widgets', 'Widgets', 'widget');

    const insights = new MenuItemContainer(
      'Insights',
      [
        new MenuItemContainer('Opportunities', [
          new MenuItemLink('insights/opportunities/top-opportunities', 'Top opportunities'),
          new MenuItemLink('insights/opportunities/improve-category-navigation', 'Improve category navigation'),
          new MenuItemLink('insights/opportunities/improve-site-search', 'Improve site search'),
        ]),
        new MenuItemLink('insights/activities', 'Activities'),
        new MenuItemLink('insights/playbooks', 'Playbooks'),
        new MenuItemContainer('Collections', [
          new MenuItemLink('insights/collections/product-collections', 'Product collections'),
          new MenuItemLink('insights/collections/category-collections', 'Category collections'),
          new MenuItemLink('insights/collections/page-collections', 'Page collections'),
          new MenuItemLink('insights/collections/query-collections', 'Query collections'),
          new MenuItemLink('insights/collections/brand-collections', 'Brand collections'),
        ]),
        new MenuItemContainer('Library', [
          new MenuItemContainer('Overview', [
            new MenuItemLink('insights/overview/sitewide-overview', 'Sitewide overview'),
            new MenuItemLink('insights/overview/site-search-overview', 'Site search overview'),
            new MenuItemLink('insights/overview/site-search-kpis', 'Site search KPIs'),
          ]),
          new MenuItemContainer('Reporting', [
            new MenuItemLink('insights/reporting/category-page-diagnostics', 'Category page diagnostics'),
            new MenuItemLink('insights/reporting/product-dashboard', 'Product dashboard'),
            new MenuItemLink('insights/reporting/brand-dashboard', 'Brand dashboard'),
            new MenuItemLink('insights/reporting/page-dashboard', 'Page dashboard'),
            new MenuItemLink('insights/reporting/site-search-diagnostics', 'Site search diagnostics'),
          ]),
        ]),
        new MenuItemContainer('Category analytics', [
          new MenuItemLink('insights/category-analytics/overall-performance', 'Overall performance'),
          new MenuItemLink('insights/category-analytics/top-categories', 'Top categories'),
        ]),
        new MenuItemContainer('Site search analytics', [
          new MenuItemLink('insights/site-search-analytics/overall-performance', 'Overall performance'),
          new MenuItemLink('insights/site-search-analytics/top-keywords', 'Top keywords'),
          new MenuItemLink('insights/site-search-analytics/no-revenue-keywords', 'No Revenue Keywords'),
        ]),
        new MenuItemContainer('SEO analytics', [
          new MenuItemLink('insights/seo-analytics/overall-performance', 'Overall performance'),
          new MenuItemLink('insights/seo-analytics/incremental', 'Incremental'),
          new MenuItemContainer('Targeted pages', [
            new MenuItemLink('insights/seo-analytics/targeted-pages/overview', 'Overview'),
            new MenuItemLink('insights/seo-analytics/targeted-pages/top-performing-pages', 'Top performing pages'),
          ]),
          new MenuItemContainer('Generated pages', [
            new MenuItemLink('insights/seo-analytics/generated-pages/overview', 'Overview'),
            new MenuItemLink('insights/seo-analytics/generated-pages/top-performing-pages', 'Top performing pages'),
          ]),
          new MenuItemContainer('Device traffic', [
            new MenuItemLink('insights/seo-analytics/device-traffic/overall', 'Overall'),
            new MenuItemLink('insights/seo-analytics/device-traffic/desktop', 'Desktop'),
            new MenuItemLink('insights/seo-analytics/device-traffic/mobile', 'Mobile'),
            new MenuItemLink('insights/seo-analytics/device-traffic/tablet', 'Tablet'),
          ]),
        ]),
        new MenuItemContainer('Widget analytics', [
          new MenuItemLink('insights/widget-analytics/just-for-you', 'Just for you'),
          new MenuItemLink('insights/widget-analytics/more-like-this', 'More like this'),
        ]),
        new MenuItemLink('insights/product-a-b-testing', 'Product A/B testing'),
        // Will be split into: Buzz, My documents, Team documents and Totals
        new MenuItemLink(
          'hippo-perspective-reportsperspective',
          'Content reports',
        ),
      ],
      'insights',
    );

    const audiences = new MenuItemContainer(
      'Audiences',
      [
        new MenuItemLink(
          'hippo-perspective-experienceoptimizerperspective',
          'Content audiences',
        ),
        new MenuItemLink('audiences/merchandising-audiences', 'Merchandising audiences'),
      ],
      'audiences',
    );

    const setup = new MenuItemContainer(
      'Setup',
      [
        new MenuItemContainer('Algorithm settings', [
          new MenuItemLink('setup/algorithm-settings/global-ranking-rules', 'Global ranking rules'),
          new MenuItemLink('setup/algorithm-settings/global-facet-management', 'Global facet management'),
        ]),
        new MenuItemLink('setup/pixel', 'Pixel'),
        new MenuItemLink('setup/feed', 'Feed'),
        new MenuItemContainer('User management', [
          new MenuItemLink('setup/user-management/sm-users', 'SM users'),
        ]),
        new MenuItemLink('hippo-perspective-adminperspective', 'System'),
        new MenuItemLink('hippo-perspective-formdataperspective', 'Formdata'),
      ],
      'settings',
    );

    return [
      home,
      experienceManager,
      projects,
      content,
      documentSearch,
      categories,
      siteSearch,
      seo,
      widgets,
      insights,
      audiences,
      this.extensions,
      setup,
    ];
  }
}
