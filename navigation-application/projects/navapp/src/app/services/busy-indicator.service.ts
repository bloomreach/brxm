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

@Injectable({
  providedIn: 'root',
})
export class BusyIndicatorService {
  private readonly source = new BehaviorSubject<boolean>(false);
  private visible: boolean;

  constructor(
  ) {
    this.source.pipe(
      distinctUntilAccumulatorIsEmpty(),
    ).subscribe(x => this.visible = x);
  }

  get isVisible(): boolean {
    return this.visible;
  }

  show(): void {
    this.source.next(true);
  }

  hide(): void {
    this.source.next(false);
  }
}
