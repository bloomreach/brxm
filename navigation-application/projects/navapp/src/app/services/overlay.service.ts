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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { distinctUntilAccumulatorIsEmpty } from '../helpers/distinct-until-equal-number-of-values';

import { ConnectionService } from './connection.service';

@Injectable({
  providedIn: 'root',
})
export class OverlayService {
  visible$: Observable<boolean>;

  constructor(
    private readonly connectionService: ConnectionService,
  ) {
    const counter = new BehaviorSubject<boolean>(false);

    this.connectionService.showMask$.subscribe(() => counter.next(true));
    this.connectionService.hideMask$.subscribe(() => counter.next(false));

    this.visible$ = counter.pipe(
      distinctUntilAccumulatorIsEmpty(),
    );
  }
}
