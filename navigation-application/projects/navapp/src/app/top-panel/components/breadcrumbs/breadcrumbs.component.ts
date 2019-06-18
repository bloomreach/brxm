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

import { BreadcrumbsService } from '../../services/breadcrumbs.service';

@Component({
  selector: 'brna-breadcrumbs',
  templateUrl: 'breadcrumbs.component.html',
  styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent {
  constructor(
    private breadcrumbsService: BreadcrumbsService,
  ) {}

  get breadcrumbs$(): Observable<string[]> {
    return this.breadcrumbsService.breadcrumbs$;
  }

  get suffix(): string {
    return this.breadcrumbsService.suffix;
  }

  onLastBreadcrumbClicked(): void {
    console.log('onLastBreadcrumbClicked');
  }
}
