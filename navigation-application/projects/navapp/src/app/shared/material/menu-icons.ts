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
import audiences from '!!raw-loader!./icons/menu/audiences.svg';
import brLogo from '!!raw-loader!./icons/menu/br-logo.svg';
import categories from '!!raw-loader!./icons/menu/categories.svg';
import defaultIcon from '!!raw-loader!./icons/menu/default.svg';
import documentSearch from '!!raw-loader!./icons/menu/document-search.svg';
import documents from '!!raw-loader!./icons/menu/documents.svg';
import experienceManager from '!!raw-loader!./icons/menu/experience-manager.svg';
import extensions from '!!raw-loader!./icons/menu/extensions.svg';
import fastTravel from '!!raw-loader!./icons/menu/fast-travel.svg';
import help from '!!raw-loader!./icons/menu/help.svg';
import home from '!!raw-loader!./icons/menu/home.svg';
import insights from '!!raw-loader!./icons/menu/insights.svg';
import projects from '!!raw-loader!./icons/menu/projects.svg';
import searchAndMerchandising from '!!raw-loader!./icons/menu/search-and-merch.svg';
import seo from '!!raw-loader!./icons/menu/seo.svg';
import settings from '!!raw-loader!./icons/menu/settings.svg';
import siteSearch from '!!raw-loader!./icons/menu/site-search.svg';
import testing from '!!raw-loader!./icons/menu/testing.svg';
import user from '!!raw-loader!./icons/menu/user.svg';
import widget from '!!raw-loader!./icons/menu/widget.svg';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

export const registerIcons = (iconRegistry: MatIconRegistry, donSanitizer: DomSanitizer) => {
  const registerIcon = (name: string, svg: string) => {
    iconRegistry.addSvgIconLiteral(name, donSanitizer.bypassSecurityTrustHtml(svg));
  };

  registerIcon('br-logo', brLogo);
  registerIcon('help', help);
  registerIcon('user', user);

  registerIcon('audiences', audiences);
  registerIcon('categories', categories);
  registerIcon('default', defaultIcon);
  registerIcon('document-search', documentSearch);
  registerIcon('documents', documents);
  registerIcon('experience-manager', experienceManager);
  registerIcon('extensions', extensions);
  registerIcon('fast-travel', fastTravel);
  registerIcon('home', home);
  registerIcon('insights', insights);
  registerIcon('projects', projects);
  registerIcon('search-and-merchandising', searchAndMerchandising);
  registerIcon('seo', seo);
  registerIcon('settings', settings);
  registerIcon('site-search', siteSearch);
  registerIcon('testing', testing);
  registerIcon('widget', widget);
};
