/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
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
      'home',
    );

    const experienceManager = new MenuItemLink(
      'hippo-perspective-channelmanagerperspective',
      'Experience Manager',
      'experience-manager',
    );

    const projects = new MenuItemLink(
      'hippo-perspective-projecstperspective',
      'Projects',
      'projects',
    );

    const content = new MenuItemLink(
      'hippo-perspective-browserperspective',
      'Content',
      'documents',
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
    ];
  }
}
