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
import { distinctUntilChanged, filter, map, shareReplay, switchMap } from 'rxjs/operators';

import { MenuStateService } from '../../main-menu/services/menu-state.service';

@Injectable()
export class BreadcrumbsService {
  private readonly suffix = new BehaviorSubject<string>('');

  constructor(
    private readonly menuStateService: MenuStateService,
  ) {}

  get breadcrumbs$(): Observable<string[]> {
    const suffix$: Observable<string> = this.suffix.pipe(
      distinctUntilChanged(),
    );

    return this.menuStateService.activePath$.pipe(
      map(path => path.map(x => x.caption)),
      filter(path => !!path.length),
      switchMap(path => suffix$.pipe(
        map(x => path.concat(x)),
      )),
      shareReplay(1),
    );
  }

  setSuffix(value: string): void {
    this.suffix.next(value || '');
  }

  clearSuffix(): void {
    this.suffix.next('');
  }
}
