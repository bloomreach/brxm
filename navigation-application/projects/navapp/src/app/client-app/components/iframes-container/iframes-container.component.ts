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

import { ClientAppService } from '../../services';

@Component({
  selector: 'brna-iframes-container',
  templateUrl: './iframes-container.component.html',
  styleUrls: ['iframes-container.component.scss'],
})
export class IframesContainerComponent implements OnInit, OnDestroy {
  private unsubscribe = new Subject();
  private activeAppURL: string;

  appURLs: string[] = [];

  constructor(private clientAppService: ClientAppService) {}

  ngOnInit(): void {
    this.clientAppService.appURLs$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(appURLs => {
        this.appURLs = appURLs;
      });

    this.clientAppService.activeAppURL$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(activeAppURL => {
        this.activeAppURL = activeAppURL;
      });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  isActive(appURL: string): boolean {
    return this.activeAppURL === appURL ? true : false;
  }
}
