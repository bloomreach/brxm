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
    const dashboard = new MenuItemLink(
      'hippo-perspective-dashboardperspective',
      'Dashboard',
      'dashboard',
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

    const siteSearch = new MenuItemContainer(
      'Site search',
      [
        new MenuItemLink('site-search/search-ranking', 'Ranking rules'),
        new MenuItemLink('site-search/redirects', 'Redirects'),
        new MenuItemLink('site-search/search-facets', 'Facets'),
        new MenuItemLink('site-search/ab-testing', 'A/B testing'),
        new MenuItemContainer('Campaigns & assets', [
          new MenuItemLink('site-search/campaigns', 'Campaigns'),
          new MenuItemLink('site-search/assets', 'Assets'),
        ]),
        new MenuItemLink('site-search/autosuggest-blacklist', 'Autosuggest blacklist'),
        new MenuItemLink('site-search/synonyms', 'Synonyms'),
        new MenuItemLink('site-search/search-ranking-diagnostics', 'Ranking diagnostics'),
        new MenuItemLink('site-search/profile-simulator', 'Profile simulator'),
      ],
      'site-search',
    );

    const categories = new MenuItemContainer(
      'Categories',
      [
        new MenuItemLink('categories/category-ranking', 'Ranking rules'),
        new MenuItemLink('categories/all-category-pages', 'All category pages'),
        new MenuItemLink('categories/category-facets-ranking', 'Facets'),
      ],
      'categories',
    );

    const seo = new MenuItemLink('seo', 'SEO', 'seo');
    const pathways = new MenuItemLink('pathways', 'Pathways', 'widget');

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
            new MenuItemLink('insights/overview/account-overview', 'Account overview'),
            new MenuItemLink('insights/overview/account-traffic-breakdown', 'Account traffic breakdown'),
            new MenuItemLink('insights/overview/sitewide-overview', 'Sitewide overview'),
            new MenuItemLink('insights/overview/site-search-overview', 'Site search overview'),
            new MenuItemLink('insights/overview/site-search-kpis', 'Site search KPIs'),
          ]),
          new MenuItemContainer('Reporting', [
            new MenuItemLink('insights/reporting/category-page-diagnostics', 'Category page diagnostics'),
            new MenuItemLink('insights/reporting/product-dashboard', 'Product dashboard'),
            new MenuItemLink('insights/reporting/brand-dashboard', 'Brand dashboard'),
            new MenuItemLink('insights/reporting/page-dashboard', 'Page dashboard'),
            new MenuItemLink('insights/reporting/neiman-category-dashboard', 'Neiman category dashboard'),
            new MenuItemLink('insights/reporting/site-search-diagnostics', 'Site search diagnostics'),
            new MenuItemLink('insights/reporting/site-search-debugging-library', 'Site search debugging library'),
            new MenuItemLink('insights/reporting/account-api-usage', 'Account API usage'),
          ]),
        ]),
        new MenuItemContainer('Category analytics', [
          new MenuItemLink('insights/category-analytics/overall-performance', 'Overall performance'),
          new MenuItemLink('insights/category-analytics/top-categories', 'Top categories'),
        ]),
        new MenuItemContainer('Site search analytics', [
          new MenuItemLink('insights/site-search-analytics/overall-performance', 'Overall performance'),
          new MenuItemLink('insights/site-search-analytics/top-keywords', 'Top keywords'),
          new MenuItemLink('insights/site-search-analytics/no-revenue-keywords', 'No revenue keywords'),
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
        new MenuItemContainer('Pathway analytics', [
          new MenuItemLink('insights/pathway-analytics/just-for-you', 'Just for you'),
          new MenuItemLink('insights/pathway-analytics/more-like-this', 'More like this'),
        ]),
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
        new MenuItemContainer('User management', [
          new MenuItemContainer('Merchandising users', [
            new MenuItemLink('setup/user-management/merchandising-user/user-management', 'User management'),
            new MenuItemLink('setup/user-management/merchandising-user/internal-user-management', 'Internal user management'),
          ]),
        ]),
        new MenuItemContainer('DevStudio', [
          new MenuItemLink('setup/devstudio/account-information', 'Account information'),
          new MenuItemLink('setup/devstudio/pixel-monitor', 'Pixel monitor'),
          new MenuItemLink('setup/devstudio/data-connect', 'DataConnect'),
          new MenuItemContainer('Feed', [
            new MenuItemLink('setup/devstudio/feed/configuration', 'Configuration'),
            new MenuItemLink('setup/devstudio/feed/status', 'Status'),
          ]),
          new MenuItemLink('setup/devstudio/attribute-configuration', 'Attribute configuration'),
          new MenuItemLink('setup/devstudio/api-explorer', 'API explorer'),
        ]),
        new MenuItemLink('hippo-perspective-adminperspective', 'System'),
        new MenuItemLink('hippo-perspective-formdataperspective', 'Formdata'),

        new MenuItemContainer('Internal admin', [
          new MenuItemLink('setup/internal-admin/api-details', 'API details'),
          new MenuItemLink('setup/internal-admin/debug-tools', 'Debug tools'),
          new MenuItemLink('setup/internal-admin/purge-cache', 'Purge cache'),
          new MenuItemLink('setup/internal-admin/merchant-custom-configs', 'Merchant custom configs'),
          new MenuItemLink('setup/internal-admin/site-configuration', 'Site configuration'),

          new MenuItemContainer('Traffic analysis', [
            new MenuItemLink('setup/internal-admin/traffic-analysis/overview', 'Overview'),
            new MenuItemLink('setup/internal-admin/traffic-analysis/last-click-overview', 'Last click overview'),
          ]),
        ]),
        new MenuItemContainer('Internal reports', [
          new MenuItemContainer('Organic trends', [
            new MenuItemLink('setup/internal-reports/organic-trends/homepage-traffic', 'Homepage traffic'),
            new MenuItemLink('setup/internal-reports/organic-trends/overall-traffic', 'Overall traffic'),
            new MenuItemLink('setup/internal-reports/organic-trends/product-page-traffic', 'Product page traffic'),
            new MenuItemLink('setup/internal-reports/organic-trends/category-page-traffic', 'Category page traffic'),
            new MenuItemLink('setup/internal-reports/organic-trends/other-page-traffic', 'Other page traffic'),
          ]),
          new MenuItemContainer('Organic', [
            new MenuItemLink('setup/internal-reports/organic/debug', 'debug'),
            new MenuItemLink('setup/internal-reports/organic/any-click-conversions', 'Any click conversions'),
            new MenuItemLink('setup/internal-reports/organic/last-click-conversions', 'Last click conversions'),
            new MenuItemLink('setup/internal-reports/organic/all-conversions', 'All conversions'),
            new MenuItemLink('setup/internal-reports/organic/account-overview', 'Account overview'),
            new MenuItemLink('setup/internal-reports/organic/traffic-sources', 'Traffic sources'),
            new MenuItemLink('setup/internal-reports/organic/legacy-debug', 'Legacy debug'),
            new MenuItemLink('setup/internal-reports/organic/site-optimization-metrics', 'Site optimization metrics'),
            new MenuItemLink('setup/internal-reports/organic/rsrp-widget-engagement', 'RS/RP widget engagement'),
            new MenuItemLink('setup/internal-reports/organic/unclaimed-thematic-traffic', 'Unclaimed thematic traffic'),
            new MenuItemLink('setup/internal-reports/organic/url-param-tracking', 'URL param tracking'),
            new MenuItemLink('setup/internal-reports/organic/all-traffic', 'All traffic'),
          ]),
          new MenuItemContainer('SNAP', [
            new MenuItemLink('setup/internal-reports/snap/multi-account-chart', 'Multi account chart'),
            new MenuItemLink('setup/internal-reports/snap/browse-sessions', 'Browse sessions'),
            new MenuItemLink('setup/internal-reports/snap/top-products', 'Top products'),
            new MenuItemLink('setup/internal-reports/snap/assisted-conversions', 'Assisted conversions'),
            new MenuItemLink('setup/internal-reports/snap/session-details', 'Session details'),
            new MenuItemLink('setup/internal-reports/snap/search-experiment', 'Search experiment'),
            new MenuItemLink('setup/internal-reports/snap/top-keywords-w-experiment', 'Top keywords w/ experiment'),
            new MenuItemLink('setup/internal-reports/snap/no-revenue-keywords-w-experiment', 'No revenue keywords w/ experiment'),
            new MenuItemLink('setup/internal-reports/snap/top-categories-w-experiment', 'Top categories w/ experiment'),
            new MenuItemLink('setup/internal-reports/snap/page-performance', 'Page performance'),
            new MenuItemLink('setup/internal-reports/snap/assisted-overview', 'Assisted overview'),
            new MenuItemLink('setup/internal-reports/snap/suggest-overview', 'Suggest overview'),
          ]),
        ]),
      ],
      'settings',
    );

    return [
      dashboard,
      experienceManager,
      projects,
      content,
      documentSearch,
      siteSearch,
      categories,
      seo,
      pathways,
      insights,
      audiences,
      this.extensions,
      setup,
    ];
  }
}
