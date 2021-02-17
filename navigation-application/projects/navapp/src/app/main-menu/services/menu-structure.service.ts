/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { TranslateService } from '@ngx-translate/core';

import { MenuItemContainer } from '../models/menu-item-container.model';
import { MenuItemLink } from '../models/menu-item-link.model';
import { MenuItem } from '../models/menu-item.model';

@Injectable()
export class MenuStructureService {
  private readonly extensions = new MenuItemContainer('Extensions', [], 'extensions');

  constructor(
    private readonly translateService: TranslateService,
  ) {}

  addExtension(item: MenuItemLink): void {
    this.extensions.children.push(item);
  }

  getMenuStructure(): MenuItem[] {
    return this.createMenuStructure();
  }

  private translate(key: string): string {
    return this.translateService.instant(key);
  }

  private createMenuStructure(): MenuItem[] {
    this.extensions.caption = this.translate('MENU_CONTAINER_EXTENSIONS');

    const dashboard = new MenuItemLink(
      'xm-home',
       this.translate('MENU_ITEM_XM_HOME'),
      'home',
    );

    const experienceManager = new MenuItemLink(
      'xm-experience-manager',
       this.translate('MENU_ITEM_XM_EXPERIENCE_MANAGER'),
      'experience-manager',
    );

    const projects = new MenuItemLink(
      'xm-projects',
       this.translate('MENU_ITEM_XM_PROJECTS'),
      'projects',
    );

    const content = new MenuItemLink(
      'xm-content',
       this.translate('MENU_ITEM_XM_CONTENT'),
      'documents',
    );

    const documentSearch = new MenuItemLink(
      'xm-document-search',
       this.translate('MENU_ITEM_XM_DOCUMENT_SEARCH'),
      'document-search',
    );

    const searchAndMerchandising = new MenuItemContainer(
      this.translate('MENU_ITEM_SEARCH_AND_MERCHANDISING'),
      [
        new MenuItemContainer(this.translate('MENU_CONTAINER_SITE_SEARCH'), [
          new MenuItemLink('search-and-merchandising/site-search/search-ranking', this.translate('MENU_ITEM_SITE_SEARCH_SEARCH_RANKING')),
          new MenuItemLink('search-and-merchandising/site-search/redirects', this.translate('MENU_ITEM_SITE_SEARCH_REDIRECTS')),
          new MenuItemLink('search-and-merchandising/site-search/search-facets', this.translate('MENU_ITEM_SITE_SEARCH_SEARCH_FACETS')),
          new MenuItemLink('search-and-merchandising/site-search/autosuggest-blacklist', this.translate('MENU_ITEM_SITE_SEARCH_AUTOSUGGEST_BLACKLIST')),
          new MenuItemLink('search-and-merchandising/site-search/synonyms', this.translate('MENU_ITEM_SITE_SEARCH_SYNONYMS')),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_CATEGORIES'), [
          new MenuItemLink('search-and-merchandising/categories/category-ranking', this.translate('MENU_ITEM_SITE_SEARCH_SEARCH_RANKING')),
          new MenuItemLink('search-and-merchandising/categories/all-category-pages', this.translate('MENU_ITEM_CATEGORIES_ALL_CATEGORY_PAGES')),
          new MenuItemLink('search-and-merchandising/categories/category-facets-ranking', this.translate('MENU_ITEM_SITE_SEARCH_SEARCH_FACETS')),
        ]),
        new MenuItemContainer(this.translate('MENU_ITEM_OPERATIONAL_TOOLS'), [
          new MenuItemContainer(this.translate('MENU_CONTAINER_CAMPAIGNS_AND_ASSETS'), [
            new MenuItemLink('search-and-merchandising/operational-tools/campaigns', this.translate('MENU_ITEM_SITE_SEARCH_CAMPAIGNS')),
            new MenuItemLink('search-and-merchandising/operational-tools/assets', this.translate('MENU_ITEM_SITE_SEARCH_ASSETS')),
          ]),
          new MenuItemLink('search-and-merchandising/operational-tools/search-ranking-diagnostics', this.translate('MENU_ITEM_SITE_SEARCH_SEARCH_RANKING_DIAGNOSTICS')),
          new MenuItemLink('search-and-merchandising/operational-tools/profile-simulator', this.translate('MENU_ITEM_SITE_SEARCH_PROFILE_SIMULATOR')),
        ]),
      ],
      'search-and-merchandising',
    );

    const seo = new MenuItemLink('seo', this.translate('MENU_ITEM_SEO'), 'seo');
    const pathways = new MenuItemLink('pathways', this.translate('MENU_ITEM_PATHWAYS'), 'widget');

    const insights = new MenuItemContainer(
       this.translate('MENU_CONTAINER_INSIGHTS'),
      [
        new MenuItemContainer(this.translate('MENU_CONTAINER_OPPORTUNITIES'), [
          new MenuItemLink('insights/opportunities/top-opportunities',
            this.translate('MENU_ITEM_INSIGHTS_OPPORTUNITIES_TOP_OPPORTUNITIES')),
          new MenuItemLink('insights/opportunities/improve-category-navigation',
            this.translate('MENU_ITEM_INSIGHTS_OPPORTUNITIES_IMPROVE_CATEGORY_NAVIGATION')),
          new MenuItemLink('insights/opportunities/improve-site-search',
            this.translate('MENU_ITEM_INSIGHTS_OPPORTUNITIES_IMPROVE_SITE_SEARCH')),
        ]),
        new MenuItemLink('insights/activities', this.translate('MENU_ITEM_INSIGHTS_ACTIVITIES')),
        new MenuItemLink('insights/playbooks', this.translate('MENU_ITEM_INSIGHTS_PLAYBOOKS')),
        new MenuItemContainer(this.translate('MENU_CONTAINER_COLLECTIONS'), [
          new MenuItemLink('insights/collections/product-collections',
            this.translate('MENU_ITEM_INSIGHTS_COLLECTIONS_PRODUCT_COLLECTIONS')),
          new MenuItemLink('insights/collections/category-collections',
            this.translate('MENU_ITEM_INSIGHTS_COLLECTIONS_CATEGORY_COLLECTIONS')),
          new MenuItemLink('insights/collections/page-collections', this.translate('MENU_ITEM_INSIGHTS_COLLECTIONS_PAGE_COLLECTIONS')),
          new MenuItemLink('insights/collections/query-collections', this.translate('MENU_ITEM_INSIGHTS_COLLECTIONS_QUERY_COLLECTIONS')),
          new MenuItemLink('insights/collections/brand-collections', this.translate('MENU_ITEM_INSIGHTS_COLLECTIONS_BRAND_COLLECTIONS')),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_LIBRARY'), [
          new MenuItemContainer(this.translate('MENU_CONTAINER_OVERVIEW'), [
            new MenuItemLink('insights/overview/sitewide-overview', this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_SITEWIDE_OVERVIEW')),
            new MenuItemLink('insights/overview/site-search-overview', this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_SITE_SEARCH_OVERVIEW')),
            new MenuItemLink('insights/overview/site-search-kpis', this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_SITE_SEARCH_KPIS')),
          ]),
          new MenuItemContainer(this.translate('MENU_CONTAINER_REPORTING'), [
            new MenuItemLink('insights/reporting/category-page-diagnostics',
              this.translate('MENU_ITEM_INSIGHTS_REPORTING_CATEGORY_PAGE_DIAGNOSTICS')),
            new MenuItemLink('insights/reporting/product-dashboard', this.translate('MENU_ITEM_INSIGHTS_REPORTING_PRODUCT_DASHBOARD')),
            new MenuItemLink('insights/reporting/brand-dashboard', this.translate('MENU_ITEM_INSIGHTS_REPORTING_BRAND_DASHBOARD')),
            new MenuItemLink('insights/reporting/page-dashboard', this.translate('MENU_ITEM_INSIGHTS_REPORTING_PAGE_DASHBOARD')),
            new MenuItemLink('insights/reporting/neiman-category-dashboard',
              this.translate('MENU_ITEM_INSIGHTS_REPORTING_NEIMAN_CATEGORY_DASHBOARD')),
            new MenuItemLink('insights/reporting/site-search-diagnostics',
              this.translate('MENU_ITEM_INSIGHTS_REPORTING_SITE_SEARCH_DIAGNOSTICS')),
            new MenuItemLink('insights/reporting/site-search-debugging-library',
              this.translate('MENU_ITEM_INSIGHTS_REPORTING_SITE_SEARCH_DEBUGGING_LIBRARY')),
          ]),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_ACCOUNT_ANALYTICS'), [
          new MenuItemLink('insights/account-analytics/account-overview', this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_ACCOUNT_OVERVIEW')),
          new MenuItemLink('insights/account-analytics/account-traffic-breakdown',
            this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_ACCOUNT_TRAFFIC_BREAKDOWN')),
          new MenuItemLink('insights/account-analytics/account-api-usage',
            this.translate('MENU_ITEM_INSIGHTS_REPORTING_ACCOUNT_API_USAGE')),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_CATEGORY_ANALYTICS'), [
          new MenuItemLink('insights/category-analytics/overall-performance',
            this.translate('MENU_ITEM_INSIGHTS_CATEGORY_ANALYTICS_OVERALL_PERFORMANCE')),
          new MenuItemLink('insights/category-analytics/top-categories',
            this.translate('MENU_ITEM_INSIGHTS_CATEGORY_ANALYTICS_TOP_CATEGORIES')),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_SITE_SEARCH_ANALYTICS'), [
          new MenuItemLink('insights/site-search-analytics/overall-performance',
            this.translate('MENU_ITEM_INSIGHTS_CATEGORY_ANALYTICS_OVERALL_PERFORMANCE')),
          new MenuItemLink('insights/site-search-analytics/top-keywords',
            this.translate('MENU_ITEM_INSIGHTS_SITE_SEARCH_ANALYTICS_TOP_KEYWORDS')),
          new MenuItemLink('insights/site-search-analytics/no-revenue-keywords',
            this.translate('MENU_ITEM_INSIGHTS_SITE_SEARCH_ANALYTICS_NO_REVENUE_KEYWORDS')),
          new MenuItemLink('insights/site-search-analytics/no-search-keywords',
            this.translate('MENU_ITEM_INSIGHTS_SITE_SEARCH_ANALYTICS_NO_SEARCH_KEYWORDS')),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_SEO_ANALYTICS'), [
          new MenuItemLink('insights/seo-analytics/overall-performance',
            this.translate('MENU_ITEM_INSIGHTS_CATEGORY_ANALYTICS_OVERALL_PERFORMANCE')),
          new MenuItemLink('insights/seo-analytics/incremental', this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_INCREMENTAL')),
          new MenuItemContainer(this.translate('MENU_CONTAINER_TARGETED_PAGES'), [
            new MenuItemLink('insights/seo-analytics/targeted-pages/overview', this.translate('MENU_CONTAINER_OVERVIEW')),
            new MenuItemLink('insights/seo-analytics/targeted-pages/top-performing-pages',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_TARGETED_PAGES_TOP_PERFORMING_PAGES')),
          ]),
          new MenuItemContainer(this.translate('MENU_CONTAINER_GENERATED_PAGES'), [
            new MenuItemLink('insights/seo-analytics/generated-pages/overview', this.translate('MENU_CONTAINER_OVERVIEW')),
            new MenuItemLink('insights/seo-analytics/generated-pages/top-performing-pages',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_TARGETED_PAGES_TOP_PERFORMING_PAGES')),
          ]),
          new MenuItemContainer(this.translate('MENU_CONTAINER_DEVICE_TRAFFIC'), [
            new MenuItemLink('insights/seo-analytics/device-traffic/overall',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_DEVICE_TRAFFIC_OVERALL')),
            new MenuItemLink('insights/seo-analytics/device-traffic/desktop',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_DEVICE_TRAFFIC_DESKTOP')),
            new MenuItemLink('insights/seo-analytics/device-traffic/mobile',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_DEVICE_TRAFFIC_MOBILE')),
            new MenuItemLink('insights/seo-analytics/device-traffic/tablet',
              this.translate('MENU_ITEM_INSIGHTS_SEO_ANALYTICS_DEVICE_TRAFFIC_TABLET')),
          ]),
        ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_PATHWAY_ANALYTICS'), [
          new MenuItemLink('insights/pathway-analytics/just-for-you', this.translate('MENU_ITEM_INSIGHTS_PATHWAY_ANALYTICS_JUST_FOR_YOU')),
          new MenuItemLink('insights/pathway-analytics/more-like-this',
            this.translate('MENU_ITEM_INSIGHTS_PATHWAY_ANALYTICS_MORE_LIKE_THIS')),
        ]),
        // Will be split into: Buzz, My documents, Team documents and Totals
        new MenuItemLink(
          'xm-content-reports',
           this.translate('MENU_ITEM_XM_CONTENT_REPORTS'),
        ),
      ],
      'insights',
    );

    const testing = new MenuItemLink(
      'testing',
      this.translate('MENU_ITEM_TESTING'),
      'testing',
    );

    const audiences = new MenuItemContainer(
       this.translate('MENU_CONTAINER_AUDIENCES'),
      [
        new MenuItemLink(
          'xm-content-audiences',
           this.translate('MENU_ITEM_XM_CONTENT_AUDIENCES'),
        ),
        new MenuItemLink('audiences/merchandising-audiences', this.translate('MENU_ITEM_AUDIENCES_MERCHANDISING_AUDIENCES')),
      ],
      'audiences',
    );

    const setup = new MenuItemContainer(
       this.translate('MENU_CONTAINER_SETUP'),
      [
        new MenuItemContainer(this.translate('MENU_CONTAINER_BRSM_GLOBAL_CONFIGURATIONS'), [
          new MenuItemLink('setup/algorithm-settings/global-ranking-rules',
            this.translate('MENU_ITEM_SETUP_ALGORITHM_SETTINGS_GLOBAL_RANKING_RULES')),
          new MenuItemLink('setup/algorithm-settings/global-facet-management',
            this.translate('MENU_ITEM_SETUP_ALGORITHM_SETTINGS_GLOBAL_FACET_MANAGEMENT')),
          new MenuItemLink('setup/algorithm-settings/api-modifier',
            this.translate('MENU_ITEM_SETUP_ALGORITHM_SETTINGS_API_MODIFIER')),
        ]),
        new MenuItemLink('setup/user-management/merchandising-user/user-management',
          this.translate('MENU_ITEM_SETUP_USER_MANAGEMENT_MERCHANDISING_USER_USER_MANAGEMENT')),
        new MenuItemLink('xm-usermanagement', this.translate('MENU_ITEM_XM_USER_MANAGEMENT')),
        new MenuItemLink('xm-api-token-management', this.translate('MENU_ITEM_XM_API_TOKEN_MANAGEMENT')),
        new MenuItemLink('xm-form-data', this.translate('MENU_ITEM_XM_FORM_DATA')),

        new MenuItemContainer(this.translate('MENU_ITEM_XM_SYSTEM'), [
          new MenuItemLink('xm-content-feed', this.translate('MENU_ITEM_XM_CONTENT_FEED')),
          new MenuItemLink('xm-replication', this.translate('MENU_ITEM_XM_REPLICATION')),
          new MenuItemLink('xm-systeminfo', this.translate('MENU_ITEM_XM_SYSTEM_INFO')),
          new MenuItemLink('xm-systemproperties', this.translate('MENU_ITEM_XM_SYSTEM_PROPERTIES')),
          new MenuItemLink('xm-systemupdater', this.translate('MENU_ITEM_XM_SYSTEM_UPDATER')),
        ]),

        new MenuItemLink('setup/data-connect', this.translate('MENU_ITEM_SETUP_DATA_CONNECT')),

        new MenuItemContainer(this.translate('MENU_CONTAINER_DEVSTUDIO'), [
          new MenuItemLink('setup/devstudio/account-information', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_ACCOUNT_INFORMATION')),
          new MenuItemLink('setup/devstudio/pixel-monitor', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_PIXEL_MONITOR')),
          new MenuItemContainer(this.translate('MENU_CONTAINER_FEED'), [
            new MenuItemLink('setup/devstudio/feed/configuration', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_FEED_CONFIGURATION')),
            new MenuItemLink('setup/devstudio/feed/status', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_FEED_STATUS')),
          ]),
          new MenuItemLink('setup/devstudio/attribute-configuration', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_ATTRIBUTE_CONFIGURATION')),
          new MenuItemLink('setup/devstudio/api-explorer', this.translate('MENU_ITEM_SETUP_DEVSTUDIO_API_EXPLORER')),
          new MenuItemContainer(this.translate('MENU_CONTAINER_DATA_CONNECT'), [
            new MenuItemLink('setup/devstudio/data-connect/content-catalogs',
              this.translate('MENU_ITEM_SETUP_DEVSTUDIO_DATA_CONNECT_CONTENT_CATALOGS')),
            new MenuItemLink('setup/devstudio/data-connect/product-catalogs',
              this.translate('MENU_ITEM_SETUP_DEVSTUDIO_DATA_CONNECT_PRODUCT_CATALOGS')),
            new MenuItemLink('setup/devstudio/data-connect/key-management',
              this.translate('MENU_ITEM_SETUP_DEVSTUDIO_DATA_CONNECT_KEY_MANAGEMENT')),
          ]),
        ]),

        new MenuItemContainer(this.translate('MENU_CONTAINER_INTERNAL_ADMIN'), [
          new MenuItemLink('setup/user-management/merchandising-user/internal-user-management',
            this.translate('MENU_ITEM_SETUP_USER_MANAGEMENT_MERCHANDISING_USER_INTERNAL_USER_MANAGEMENT')),
          new MenuItemLink('setup/internal-admin/brx-integration-configs',
            this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_BRX_INTEGRATION_CONFIGS')),
          new MenuItemLink('setup/internal-admin/api-details', this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_API_DETAILS')),
          new MenuItemLink('setup/internal-admin/debug-tools', this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_DEBUG_TOOLS')),
          new MenuItemLink('setup/internal-admin/purge-cache', this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_PURGE_CACHE')),
          new MenuItemLink('setup/internal-admin/merchant-custom-configs',
            this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_MERCHANT_CUSTOM_CONFIGS')),
          new MenuItemLink('setup/internal-admin/site-configuration', this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_SITE_CONFIGURATION')),

          new MenuItemContainer(this.translate('MENU_CONTAINER_TRAFFIC_ANALYSIS'), [
            new MenuItemLink('setup/internal-admin/traffic-analysis/overview', this.translate('MENU_CONTAINER_OVERVIEW')),
            new MenuItemLink('setup/internal-admin/traffic-analysis/last-click-overview',
              this.translate('MENU_ITEM_SETUP_INTERNAL_ADMIN_TRAFFIC_ANALYSIS_LAST_CLICK_OVERVIEW')),
          ]),
      ]),
        new MenuItemContainer(this.translate('MENU_CONTAINER_INTERNAL_REPORTS'), [
          new MenuItemContainer(this.translate('MENU_CONTAINER_ORGANIC_TRENDS'), [
            new MenuItemLink('setup/internal-reports/organic-trends/homepage-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRENDS_HOMEPAGE_TRAFFIC')),
            new MenuItemLink('setup/internal-reports/organic-trends/overall-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRENDS_OVERALL_TRAFFIC')),
            new MenuItemLink('setup/internal-reports/organic-trends/product-page-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRENDS_PRODUCT_PAGE_TRAFFIC')),
            new MenuItemLink('setup/internal-reports/organic-trends/category-page-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRENDS_CATEGORY_PAGE_TRAFFIC')),
            new MenuItemLink('setup/internal-reports/organic-trends/other-page-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRENDS_OTHER_PAGE_TRAFFIC')),
          ]),
          new MenuItemContainer(this.translate('MENU_CONTAINER_ORGANIC'), [
            new MenuItemLink('setup/internal-reports/organic/debug', this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_DEBUG')),
            new MenuItemLink('setup/internal-reports/organic/any-click-conversions',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_ANY_CLICK_CONVERSIONS')),
            new MenuItemLink('setup/internal-reports/organic/last-click-conversions',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_LAST_CLICK_CONVERSIONS')),
            new MenuItemLink('setup/internal-reports/organic/all-conversions',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_ALL_CONVERSIONS')),
            new MenuItemLink('setup/internal-reports/organic/account-overview',
              this.translate('MENU_ITEM_INSIGHTS_OVERVIEW_ACCOUNT_OVERVIEW')),
            new MenuItemLink('setup/internal-reports/organic/traffic-sources',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_TRAFFIC_SOURCES')),
            new MenuItemLink('setup/internal-reports/organic/legacy-debug',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_LEGACY_DEBUG')),
            new MenuItemLink('setup/internal-reports/organic/site-optimization-metrics',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_SITE_OPTIMIZATION_METRICS')),
            new MenuItemLink('setup/internal-reports/organic/rsrp-widget-engagement',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_RSRP_WIDGET_ENGAGEMENT')),
            new MenuItemLink('setup/internal-reports/organic/unclaimed-thematic-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_UNCLAIMED_THEMATIC_TRAFFIC')),
            new MenuItemLink('setup/internal-reports/organic/url-param-tracking',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_URL_PARAM_TRACKING')),
            new MenuItemLink('setup/internal-reports/organic/all-traffic',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_ORGANIC_ALL_TRAFFIC')),
          ]),
          new MenuItemContainer(this.translate('MENU_CONTAINER_SNAP'), [
            new MenuItemLink('setup/internal-reports/snap/multi-account-chart',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_MULTI_ACCOUNT_CHART')),
            new MenuItemLink('setup/internal-reports/snap/browse-sessions',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_BROWSE_SESSIONS')),
            new MenuItemLink('setup/internal-reports/snap/top-products',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_TOP_PRODUCTS')),
            new MenuItemLink('setup/internal-reports/snap/assisted-conversions',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_ASSISTED_CONVERSIONS')),
            new MenuItemLink('setup/internal-reports/snap/session-details',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_SESSION_DETAILS')),
            new MenuItemLink('setup/internal-reports/snap/search-experiment',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_SEARCH_EXPERIMENT')),
            new MenuItemLink('setup/internal-reports/snap/top-keywords-w-experiment',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_TOP_KEYWORDS_W_EXPERIMENT')),
            new MenuItemLink('setup/internal-reports/snap/no-revenue-keywords-w-experiment',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_NO_REVENUE_KEYWORDS_W_EXPERIMENT')),
            new MenuItemLink('setup/internal-reports/snap/top-categories-w-experiment',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_TOP_CATEGORIES_W_EXPERIMENT')),
            new MenuItemLink('setup/internal-reports/snap/page-performance',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_PAGE_PERFORMANCE')),
            new MenuItemLink('setup/internal-reports/snap/assisted-overview',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_ASSISTED_OVERVIEW')),
            new MenuItemLink('setup/internal-reports/snap/suggest-overview',
              this.translate('MENU_ITEM_SETUP_INTERNAL_REPORTS_SNAP_SUGGEST_OVERVIEW')),
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
      searchAndMerchandising,
      seo,
      pathways,
      insights,
      testing,
      audiences,
      this.extensions,
      setup,
    ];
  }
}
