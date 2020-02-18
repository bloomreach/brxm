/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Inject, Injectable } from '@angular/core';
import { merge, of, Subject } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { catchError, mapTo, skipUntil, startWith } from 'rxjs/operators';

import { APP_BOOTSTRAPPED } from '../bootstrap/app-bootstrapped';

@Injectable({
  providedIn: 'root',
})
export class MainLoaderService {
  private readonly source = new Subject<boolean>();
  private visible: boolean;

  constructor(@Inject(APP_BOOTSTRAPPED) private readonly appBootstrapped: Promise<void>) {
    const appBootstrapped$ = fromPromise(this.appBootstrapped).pipe(
      mapTo(false),
      catchError(() => of(false)),
    );

    merge(
      appBootstrapped$.pipe(
        startWith(true),
      ),
      this.source.pipe(
        skipUntil(appBootstrapped$),
      ),
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
