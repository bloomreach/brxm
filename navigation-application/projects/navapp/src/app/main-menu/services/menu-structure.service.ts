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

import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

@Injectable()
export class MenuStructureService {
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
      'Experience Manager',
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

    const settings = new MenuItemLink(
      'hippo-perspective-adminperspective',
      'Settings',
      'settings',
    );

    const documentSearch = new MenuItemLink(
      'document-search',
      'Document Search',
      'documents',
    );

    const categories = new MenuItemContainer(
      'Categories',
      [
        new MenuItemLink(
          'category-ranking',
          'Category Ranking',
        ),
        new MenuItemLink(
          'all-category-pages',
          'All Category Pages',
        ),
        new MenuItemLink(
          'category-ranking-diagnostics',
          'Category Ranking Diagnostics',
        ),
        new MenuItemLink(
          'category-facets-ranking',
          'Category Facets',
        ),
        new MenuItemLink(
          'category-banners',
          'Category Banners',
        ),
      ],
      'categories',
    );

    const insights = new MenuItemContainer(
      'Insights',
      [
        new MenuItemContainer(
          'Opportunities',
          [
            new MenuItemLink(
              'top-opportunities',
              'Top Opportunities',
            ),
            new MenuItemLink(
              'improve-category-navigation',
              'Improve Category Navigation',
            ),
            new MenuItemLink(
              'improve-site-search',
              'Improve Site Search',
            ),
          ],
        ),
        new MenuItemLink(
          'activities',
          'Activities',
        ),
        new MenuItemLink(
          'playbooks',
          'Playbooks',
        ),
        new MenuItemContainer(
          'Collections',
          [
            new MenuItemLink(
              'product-collections',
              'Product Collections',
            ),
            new MenuItemLink(
              'category-collections',
              'Category Collections',
            ),
            new MenuItemLink(
              'page-collections',
              'Page Collections',
            ),
            new MenuItemLink(
              'query-collections',
              'Query Collections',
            ),
            new MenuItemLink(
              'brand-collections',
              'Brand Collections',
            ),
          ],
        ),
        new MenuItemContainer(
          'Library',
          [
            new MenuItemLink(
              'hippo-perspective-reportsperspective',
              'Content Reports',
            ),
          ],
        ),
        new MenuItemLink(
          'product-a-b-testing',
          'Product A/B testing',
        ),
      ],
      'insights',
    );

    return [
      home,
      experienceManager,
      projects,
      content,
      documentSearch,
      categories,
      insights,
      settings,
    ];
  }
}
