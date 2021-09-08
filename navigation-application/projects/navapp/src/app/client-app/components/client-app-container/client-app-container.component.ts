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

import { animate, state, style, transition, trigger } from '@angular/animations';
import { ChangeDetectorRef, Component, OnDestroy, OnInit, QueryList, ViewChildren } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { NavigationService } from '../../../services/navigation.service';
import { ClientAppService } from '../../services/client-app.service';
import { ClientAppComponent } from '../client-app/client-app.component';

@Component({
  selector: 'brna-client-app-container',
  templateUrl: './client-app-container.component.html',
  styleUrls: ['client-app-container.component.scss'],
  animations: [
    trigger('fadeInOut', [
      state('true', style({
        opacity: .25,
        display: 'block',
      })),
      state('false', style({
        opacity: 0,
        display: 'none',
      })),
      transition('false => true', [
        style({ display: 'block' }),
        animate('200ms ease-in'),
      ]),
      transition('true => false', animate('200ms ease-out')),
    ]),
  ],
})
export class ClientAppContainerComponent implements OnInit, OnDestroy {
  unsubscribe = new Subject<void>();
  urls: string[] = [];

  @ViewChildren(ClientAppComponent)
  clientAppComponents: QueryList<ClientAppComponent>;

  constructor(
    private readonly clientAppService: ClientAppService,
    private readonly navigationService: NavigationService,
    private readonly cd: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.clientAppService.urls$
    .pipe(takeUntil(this.unsubscribe))
    .subscribe(urls => {
      this.urls = urls;
      this.cd.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  get isNavigating$(): Observable<boolean> {
    return this.navigationService.navigating$;
  }

  get isLoaderShowed(): boolean {
    const allAppAreInactive = !this.urls.some(url => this.isActive(url));

    return allAppAreInactive;
  }

  isActive(appURL: string): boolean {
    const activeApp = this.clientAppService.activeApp;

    return activeApp ? activeApp.url === appURL : false;
  }
}
