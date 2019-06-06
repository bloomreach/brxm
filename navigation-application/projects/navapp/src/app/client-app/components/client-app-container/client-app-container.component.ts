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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services';

@Component({
  selector: 'brna-client-app-container',
  templateUrl: './client-app-container.component.html',
  styleUrls: ['client-app-container.component.scss'],
})
export class ClientAppContainerComponent implements OnInit, OnDestroy {
  private unsubscribe = new Subject();
  private activeAppId: string;

  apps: ClientApp[] = [];

  constructor(private clientAppService: ClientAppService) {}

  ngOnInit(): void {
    this.clientAppService.apps$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(apps => {
        this.apps = apps;
      });

    this.clientAppService.activeAppId$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(activeAppId => {
        this.activeAppId = activeAppId;
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  isActive(appURL: string): boolean {
    return this.activeAppId === appURL;
  }

  trackByAppIdFn(index: number, app: ClientApp): string {
    return app.id;
  }
}
