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
import { Observable, ReplaySubject, throwError } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { catchError, first, tap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';

import { NavConfigService } from './nav-config.service';

@Injectable({
  providedIn: 'root',
})
export class BootstrapService {
  private bootstrapped = false;
  private bootstrappedSuccessful = new ReplaySubject<void>(1);

  constructor(
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
  ) {}

  get bootstrappedSuccessful$(): Observable<void> {
    return this.bootstrappedSuccessful.asObservable();
  }

  bootstrap(): Observable<void> {
    if (this.bootstrapped) {
      return;
    }

    this.bootstrapped = true;

    return fromPromise(Promise.all([
      this.navConfigService.init(),
      this.clientAppService.init(),
    ])).pipe(
      catchError(error => throwError(`[NAVAPP] Bootstrap error: ${error}`)),
      first(),
      tap(() => {
        this.bootstrappedSuccessful.next();
        this.bootstrappedSuccessful.complete();
      }),
    ) as Observable<void>;
  }
}
