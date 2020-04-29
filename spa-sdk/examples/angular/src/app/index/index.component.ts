/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, InjectionToken, Inject, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { BrPageComponent } from '@bloomreach/ng-sdk';

import { BannerComponent } from '../banner/banner.component';
import { ContentComponent } from '../content/content.component';
import { MenuComponent } from '../menu/menu.component';
import { NewsListComponent } from '../news-list/news-list.component';

export const CMS_BASE_URL = new InjectionToken<string>('brXM Base URL');
export const SPA_BASE_URL = new InjectionToken<string>('SPA Base URL');

@Component({
  selector: 'br-index',
  templateUrl: './index.component.html',
  styleUrls: ['./index.component.css'],
})
export class IndexComponent implements OnInit {
  configuration: BrPageComponent['configuration'];

  mapping = {
    menu: MenuComponent,
    Banner: BannerComponent,
    Content: ContentComponent,
    'News List': NewsListComponent,
  };

  private navigationEnd: Observable<NavigationEnd>;

  constructor(
    router: Router,
    @Inject(CMS_BASE_URL) cmsBaseUrl: string,
    @Inject(SPA_BASE_URL) spaBaseUrl: string,
  ) {
    this.configuration = {
      cmsBaseUrl,
      spaBaseUrl,
      request: { path: router.url },
    } as BrPageComponent['configuration'];

    this.navigationEnd = router.events.pipe(
      filter(event => event instanceof NavigationEnd),
    ) as Observable<NavigationEnd>;
  }

  ngOnInit() {
    this.navigationEnd.subscribe((event) => {
      this.configuration = {
        ...this.configuration,
        request: { path: event.url },
      };
    });
  }
}
