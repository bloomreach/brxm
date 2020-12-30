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

// tslint:disable:match-default-export-name
import audiencesActive from '!!raw-loader!./icons/menu/audiences.highlighted.svg';
import audiences from '!!raw-loader!./icons/menu/audiences.svg';
import brLogo from '!!raw-loader!./icons/menu/br-logo.svg';
import categoriesActive from '!!raw-loader!./icons/menu/categories.highlighted.svg';
import categories from '!!raw-loader!./icons/menu/categories.svg';
import defaultIconActive from '!!raw-loader!./icons/menu/default.highlighted.svg';
import defaultIcon from '!!raw-loader!./icons/menu/default.svg';
import documentSearchActive from '!!raw-loader!./icons/menu/document-search.highlighted.svg';
import documentSearch from '!!raw-loader!./icons/menu/document-search.svg';
import documentsActive from '!!raw-loader!./icons/menu/documents.highlighted.svg';
import documents from '!!raw-loader!./icons/menu/documents.svg';
import experienceManagerActive from '!!raw-loader!./icons/menu/experience-manager.highlighted.svg';
import experienceManager from '!!raw-loader!./icons/menu/experience-manager.svg';
import extensionsActive from '!!raw-loader!./icons/menu/extensions.highlighted.svg';
import extensions from '!!raw-loader!./icons/menu/extensions.svg';
import fastTravelActive from '!!raw-loader!./icons/menu/fast-travel.highlighted.svg';
import fastTravel from '!!raw-loader!./icons/menu/fast-travel.svg';
import help from '!!raw-loader!./icons/menu/help.svg';
import homeActive from '!!raw-loader!./icons/menu/home.highlighted.svg';
import home from '!!raw-loader!./icons/menu/home.svg';
import insightsActive from '!!raw-loader!./icons/menu/insights.highlighted.svg';
import insights from '!!raw-loader!./icons/menu/insights.svg';
import projectsActive from '!!raw-loader!./icons/menu/projects.highlighted.svg';
import projects from '!!raw-loader!./icons/menu/projects.svg';
import searchAndMerchandisingActive from '!!raw-loader!./icons/menu/search-and-merch.highlighted.svg';
import searchAndMerchandising from '!!raw-loader!./icons/menu/search-and-merch.svg';
import seoActive from '!!raw-loader!./icons/menu/seo.highlighted.svg';
import seo from '!!raw-loader!./icons/menu/seo.svg';
import settingsActive from '!!raw-loader!./icons/menu/settings.highlighted.svg';
import settings from '!!raw-loader!./icons/menu/settings.svg';
import siteSearchActive from '!!raw-loader!./icons/menu/site-search.highlighted.svg';
import siteSearch from '!!raw-loader!./icons/menu/site-search.svg';
import testingActive from '!!raw-loader!./icons/menu/testing.highlighted.svg';
import testing from '!!raw-loader!./icons/menu/testing.svg';
import user from '!!raw-loader!./icons/menu/user.svg';
import widgetActive from '!!raw-loader!./icons/menu/widget.highlighted.svg';
import widget from '!!raw-loader!./icons/menu/widget.svg';
import { MatIconRegistry } from '@angular/material';
import { DomSanitizer } from '@angular/platform-browser';

export const registerIcons = (iconRegistry: MatIconRegistry, donSanitizer: DomSanitizer) => {
  const registerIcon = (name: string, svg: string) => {
    iconRegistry.addSvgIconLiteral(name, donSanitizer.bypassSecurityTrustHtml(svg));
  };

  registerIcon('br-logo', brLogo);
  registerIcon('help', help);
  registerIcon('user', user);

  registerIcon('audiences', audiences);
  registerIcon('audiences.highlighted', audiencesActive);

  registerIcon('categories', categories);
  registerIcon('categories.highlighted', categoriesActive);

  registerIcon('default', defaultIcon);
  registerIcon('default.highlighted', defaultIconActive);

  registerIcon('document-search', documentSearch);
  registerIcon('document-search.highlighted', documentSearchActive);

  registerIcon('documents', documents);
  registerIcon('documents.highlighted', documentsActive);

  registerIcon('experience-manager', experienceManager);
  registerIcon('experience-manager.highlighted', experienceManagerActive);

  registerIcon('extensions', extensions);
  registerIcon('extensions.highlighted', extensionsActive);

  registerIcon('fast-travel', fastTravel);
  registerIcon('fast-travel.highlighted', fastTravelActive);

  registerIcon('home', home);
  registerIcon('home.highlighted', homeActive);

  registerIcon('insights', insights);
  registerIcon('insights.highlighted', insightsActive);

  registerIcon('projects', projects);
  registerIcon('projects.highlighted', projectsActive);

  registerIcon('api-token-management', projects);
  registerIcon('api-token-management.highlighted', projectsActive);

  registerIcon('search-and-merchandising', searchAndMerchandising);
  registerIcon('search-and-merchandising.highlighted', searchAndMerchandisingActive);

  registerIcon('seo', seo);
  registerIcon('seo.highlighted', seoActive);

  registerIcon('settings', settings);
  registerIcon('settings.highlighted', settingsActive);

  registerIcon('site-search', siteSearch);
  registerIcon('site-search.highlighted', siteSearchActive);

  registerIcon('testing', testing);
  registerIcon('testing.highlighted', testingActive);

  registerIcon('widget', widget);
  registerIcon('widget.highlighted', widgetActive);
};
