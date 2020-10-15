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
import { RESPONSE, REQUEST } from '@nguniversal/express-engine/tokens';
import { Observable } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Response, Request } from 'express';
import { serialize } from 'cookie';
import { BrPageComponent } from '@bloomreach/ng-sdk';
import { Page } from '@bloomreach/spa-sdk';

import { BannerComponent } from '../banner/banner.component';
import { ContentComponent } from '../content/content.component';
import { MenuComponent } from '../menu/menu.component';
import { NewsListComponent } from '../news-list/news-list.component';

export const BASE_URL = new InjectionToken<string>('SPA Base URL');
export const ENDPOINT = new InjectionToken<string>('brXM API endpoint');

const VISITOR_COOKIE = '_v';
const VISITOR_COOKIE_MAX_AGE_IN_SECONDS = 365 * 24 * 60 * 60;

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
    @Inject(BASE_URL) baseUrl?: string,
    @Inject(ENDPOINT) endpoint?: string,
    @Inject(REQUEST) @Optional() private request?: Request,
    @Inject(RESPONSE) @Optional() private response?: Response,
  ) {
    this.configuration = {
      baseUrl,
      endpoint,
      endpointQueryParameter: 'endpoint',
      request: {
        connection: request?.connection,
        headers: request?.headers['x-forwarded-for']
          ? { 'x-forwarded-for': request?.headers['x-forwarded-for'] }
          : undefined,
        path: router.url,
      },
      visitor: this.request?.cookies[VISITOR_COOKIE] && JSON.parse(this.request.cookies[VISITOR_COOKIE]),
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

  setVisitor(page?: Page) {
    const visitor = page?.getVisitor();

    if (!visitor) {
      return;
    }

    this.configuration.visitor = visitor;
    this.response?.setHeader(
      'Set-Cookie',
      serialize(
        VISITOR_COOKIE,
        JSON.stringify(visitor),
        { httpOnly: true, maxAge: VISITOR_COOKIE_MAX_AGE_IN_SECONDS },
      ),
    );
  }
}
