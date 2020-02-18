/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Injectable, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { distinctUntilAccumulatorIsEmpty } from '../helpers/distinct-until-equal-number-of-values';

import { ConnectionService } from './connection.service';

@Injectable({
  providedIn: 'root',
})
export class OverlayService implements OnDestroy {
  private readonly unsubscribe = new Subject<void>();
  private visible = false;

  constructor(
    private readonly connectionService: ConnectionService,
  ) {
    const visible$ = new Subject<boolean>();

    this.connectionService.showMask$.subscribe(() => visible$.next(true));
    this.connectionService.hideMask$.subscribe(() => visible$.next(false));

    visible$.pipe(
      takeUntil(this.unsubscribe),
      distinctUntilAccumulatorIsEmpty(),
    ).subscribe(x => this.visible = x);
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  get isVisible(): boolean {
    return this.visible;
  }
}
