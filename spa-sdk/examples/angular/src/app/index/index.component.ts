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

import { Component, InjectionToken, Inject, OnInit, Optional } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { REQUEST } from '@nguniversal/express-engine/tokens';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Request } from 'express';
import { BrPageComponent } from '@bloomreach/ng-sdk';
import { Page } from '@bloomreach/spa-sdk';

import { BannerComponent } from '../banner/banner.component';
import { ContentComponent } from '../content/content.component';
import { MenuComponent } from '../menu/menu.component';
import { NewsListComponent } from '../news-list/news-list.component';

export const ENDPOINT = new InjectionToken<string>('brXM API endpoint');

@Component({
  selector: 'br-index',
  templateUrl: './index.component.html',
})
export class IndexComponent implements OnInit {
  configuration: BrPageComponent['configuration'];

  mapping = {
    menu: MenuComponent,
    Banner: BannerComponent,
    Content: ContentComponent,
    'News List': NewsListComponent,
    'Simple Content': ContentComponent,
  };

  private navigationEnd: Observable<NavigationEnd>;

  constructor(
    router: Router,
    @Inject(ENDPOINT) endpoint?: string,
    @Inject(REQUEST) @Optional() request?: Request,
  ) {
    this.configuration = {
      endpoint,
      request,
      endpointQueryParameter: 'endpoint',
      path: router.url,
    } as IndexComponent['configuration'];

    this.navigationEnd = router.events.pipe(
      filter(event => event instanceof NavigationEnd),
    ) as Observable<NavigationEnd>;
  }

  ngOnInit() {
    this.navigationEnd.subscribe((event) => {
      this.configuration = { ...this.configuration, path: event.url };
    });
  }

  setVisitor(page?: Page) {
    this.configuration.visitor = page?.getVisitor();
  }
}
