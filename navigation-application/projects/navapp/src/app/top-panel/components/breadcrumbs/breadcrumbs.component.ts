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

import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { DeepLinkingService } from '../../../routing/deep-linking.service';
import { BreadcrumbsService } from '../../services/breadcrumbs.service';

@Component({
  selector: 'brna-breadcrumbs',
  templateUrl: 'breadcrumbs.component.html',
  styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent {
  constructor(
    private breadcrumbsService: BreadcrumbsService,
    private deepLinkingService: DeepLinkingService,
  ) {}

  get breadcrumbs$(): Observable<string[]> {
    return this.breadcrumbsService.breadcrumbs$.pipe(
      map(x => x.slice(0, x.length - 1)),
    );
  }

  get suffix$(): Observable<string> {
    return this.breadcrumbsService.breadcrumbs$.pipe(
      map(x => x[x.length - 1]),
    );
  }

  onLastBreadcrumbClicked(): void {
    this.deepLinkingService.navigateToDefaultCurrentAppPage();
  }
}
