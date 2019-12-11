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

import { Inject, Injectable, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { map, takeUntil, throttleTime } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';

import { USER_ACTIVITY_DEBOUNCE_TIME } from './user-activity-debounce-time';

@Injectable({
  providedIn: 'root',
})
export class UserActivityService implements OnDestroy {
  private readonly event$ = new Subject<ClientApp>();
  private readonly unsubscribe = new Subject();

  constructor(
    private readonly clientAppService: ClientAppService,
    @Inject(USER_ACTIVITY_DEBOUNCE_TIME) private readonly userActivityDebounceTime,
  ) {
    if (!this.userActivityDebounceTime) {
      throw new Error('USER_ACTIVITY_DEBOUNCE_TIME must be set');
    }

    this.startBroadcasting();
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  broadcastUserActivity(): void {
    this.event$.next(this.clientAppService.activeApp);
  }

  private startBroadcasting(): void {
    this.event$.pipe(
      takeUntil(this.unsubscribe),
      throttleTime(this.userActivityDebounceTime),
      map(activeApp => this.clientAppService.apps.filter(app => app !== activeApp)),
      map(apps => apps.filter(app => !!app.api.onUserActivity)),
    ).subscribe(apps => apps.forEach(app => app.api.onUserActivity()));
  }
}
