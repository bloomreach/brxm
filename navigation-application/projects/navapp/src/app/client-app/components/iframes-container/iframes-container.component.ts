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

import { ClientApplicationConfiguration } from '../../models';
import { ClientAppService } from '../../services';

@Component({
  selector: 'brna-iframes-container',
  templateUrl: './iframes-container.component.html',
  styleUrls: ['iframes-container.component.scss'],
})
export class IframesContainerComponent implements OnInit, OnDestroy {
  private unsubscribe = new Subject();
  private activeAppId: string;

  appConfigs: ClientApplicationConfiguration[] = [];

  constructor(private clientAppService: ClientAppService) {}

  ngOnInit(): void {
    this.clientAppService.appConfigs$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(appConfigs => {
        this.appConfigs = appConfigs;
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

  isActive(appConfig: ClientApplicationConfiguration): boolean {
    return this.activeAppId === appConfig.id ? true : false;
  }

  getConfigId(index: number, config: ClientApplicationConfiguration): string {
    return config.id;
  }
}
