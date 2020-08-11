/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { Title } from '@angular/platform-browser';
import { map } from 'rxjs/operators';

import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

@Injectable({
  providedIn: 'root',
})
export class PageTitleManagerService {
  private initialPageTitle: string;

  constructor(
    private readonly titleService: Title,
    private readonly breadcrumbsService: BreadcrumbsService,
  ) { }

  init(): void {
    this.initialPageTitle = this.titleService.getTitle();
    this.trackBreadcrumbLabelAndSetAsPageTitle();
  }

  private setPageSubTitle(subTitle: string): void {
    const title = `${this.initialPageTitle} | ${subTitle || ''}`;

    this.titleService.setTitle(title);
  }

  private trackBreadcrumbLabelAndSetAsPageTitle(): void {
    this.breadcrumbsService.breadcrumbs$.pipe(
      map(breadcrumbs => {
        const suffix = breadcrumbs[breadcrumbs.length - 1];
        const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 2];

        return suffix || lastBreadcrumb;
      }),
    ).subscribe(pageSubTitle => this.setPageSubTitle(pageSubTitle));
  }
}
