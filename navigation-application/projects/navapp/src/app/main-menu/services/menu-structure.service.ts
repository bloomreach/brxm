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
  private extensions = new MenuItemContainer(
    'Extensions', [], 'extensions',
  );

  addExtension(item: MenuItemLink): void {
    this.extensions.children.push(item);
  }

  getMenuStructure(): MenuItem[] {
    return this.createMenuStructure();
  }

  private createMenuStructure(): MenuItem[] {
    const dashboard = new MenuItemLink(
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

    const setup = new MenuItemContainer(
      'Setup',
      [
        new MenuItemLink('hippo-perspective-adminperspective', 'System'),
        new MenuItemLink('hippo-perspective-formdataperspective', 'Formdata'),
      ],
      'settings',
    );

    const documentSearch = new MenuItemLink(
      'hippo-perspective-searchperspective',
      'Document search',
      'document-search',
    );

    const categories = new MenuItemContainer(
      'Categories',
      [
        new MenuItemLink('category-ranking', 'Category ranking'),
        new MenuItemLink('all-category-pages', 'All category pages'),
        new MenuItemLink(
          'category-ranking-diagnostics',
          'Category Ranking Diagnostics',
        ),
        new MenuItemLink('category-facets-ranking', 'Category Facets'),
        new MenuItemLink('category-banners', 'Category banners'),
      ],
      'categories',
    );

    const seo = new MenuItemLink(
      'seo',
      'SEO',
      'seo',
    );
    const insights = new MenuItemContainer(
      'Insights',
      [
        new MenuItemContainer('Opportunities', [
          new MenuItemLink('top-opportunities', 'Top opportunities'),
          new MenuItemLink(
            'improve-category-navigation',
            'Improve category navigation',
          ),
          new MenuItemLink('improve-site-search', 'Improve site search'),
        ]),
        new MenuItemLink('activities', 'Activities'),
        new MenuItemLink('playbooks', 'Playbooks'),
        new MenuItemContainer('Collections', [
          new MenuItemLink('product-collections', 'Product collections'),
          new MenuItemLink('category-collections', 'Category collections'),
          new MenuItemLink('page-collections', 'Page collections'),
          new MenuItemLink('query-collections', 'Query collections'),
          new MenuItemLink('brand-collections', 'Brand collections'),
        ]),
        new MenuItemContainer('Library', [
          new MenuItemLink(
            'hippo-perspective-reportsperspective',
            'Content Reports',
          ),
        ]),
        new MenuItemLink('product-a-b-testing', 'Product A/B testing'),
      ],
      'insights',
    );

    const audiences = new MenuItemContainer(
      'Audiences',
      [
        new MenuItemLink('hippo-perspective-experienceoptimizerperspective', 'Content audiences'),
        new MenuItemLink('merchandising-audiences', 'Merchandising audiences'),
      ],
      'audiences',
    );

    const experienceManager2 = new MenuItemLink(
      'experience-manager-2',
      'Experience Manager 2',
      'experience-manager',
    );

    return [
      dashboard,
      experienceManager,
      projects,
      content,
      documentSearch,
      categories,
      seo,
      insights,
      audiences,
      this.extensions,
      setup,
      experienceManager2,
    ];
  }
}
