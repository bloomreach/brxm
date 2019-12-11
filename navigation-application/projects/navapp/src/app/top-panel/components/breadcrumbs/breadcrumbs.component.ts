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

import { Component, OnInit } from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { NavigationService } from '../../../services/navigation.service';
import { BreadcrumbsService } from '../../services/breadcrumbs.service';

@Component({
  selector: 'brna-breadcrumbs',
  templateUrl: 'breadcrumbs.component.html',
  styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent implements OnInit {
  breadcrumbsWithoutSuffix: string[] = [];
  suffix = '';

  private readonly unsubscribe = new Subject();

  constructor(
    private readonly breadcrumbsService: BreadcrumbsService,
    private readonly navigationService: NavigationService,
  ) { }

  ngOnInit(): void {
    this.breadcrumbsService.breadcrumbs$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(breadcrumbs => {
      this.breadcrumbsWithoutSuffix = breadcrumbs.slice(0, breadcrumbs.length - 1);
      this.suffix = breadcrumbs[breadcrumbs.length - 1];
    });
  }

  onLastBreadcrumbClicked(): void {
    if (!this.suffix) {
      return;
    }

    this.navigationService.navigateToDefaultAppPage(NavigationTrigger.Breadcrumbs);
  }
}
