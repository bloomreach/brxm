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

import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { MatSidenav } from '@angular/material';
import { TranslateService } from '@ngx-translate/core';
import { Observable, of, Subject } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { catchError, mapTo, startWith, takeUntil } from 'rxjs/operators';

import { APP_BOOTSTRAPPED } from './bootstrap/app-bootstrapped';
import { AppError } from './error-handling/models/app-error';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { UserSettings } from './models/dto/user-settings.dto';
import { OverlayService } from './services/overlay.service';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

@Component({
  selector: 'brna-root',
  templateUrl: './app.component.html',
  styleUrls: [ 'app.component.scss' ],
  animations: [
    trigger('showOverlay', [
      state('false', style({ opacity: '0', 'z-index': 0 })),
      state('true', style({ opacity: '1', 'z-index': 3 })),
      transition('false <=> true', [
        animate('400ms cubic-bezier(.25, .8, .25, 1)'),
      ]),
    ]),
    trigger('stackMain', [
      state('false', style({ 'z-index': 0 })),
      state('true', style({ 'z-index': 4 })),
      transition('false <=> true', [
        animate('400ms cubic-bezier(.25, .8, .25, 1)'),
      ]),
    ]),
  ],
})
export class AppComponent implements OnInit, OnDestroy {
  @HostBinding('class.mat-typography')
  typography = true;

  @ViewChild(MatSidenav, { static: true })
  sidenav: MatSidenav;

  isLoading$: Observable<boolean>;
  isOverlayVisible = false;
  private unsubscribe = new Subject();

  constructor(
    private translateService: TranslateService,
    private overlayService: OverlayService,
    private rightSidePanelService: RightSidePanelService,
    private errorHandlingService: ErrorHandlingService,
    @Inject(USER_SETTINGS) private userSettings: UserSettings,
    @Inject(APP_BOOTSTRAPPED) private appBootstrapped: Promise<void>,
  ) { }

  get error(): AppError {
    return this.errorHandlingService.currentError;
  }

  ngOnInit(): void {
    this.initializeObservables();
    this.configureTranslateService();
    this.rightSidePanelService.setSidenav(this.sidenav);
    this.overlayService.visible$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(visible => this.isOverlayVisible = visible);
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  private initializeObservables(): void {
    this.isLoading$ = fromPromise(this.appBootstrapped).pipe(
      mapTo(false),
      startWith(true),
      catchError(() => of(false)),
    );
  }

  private configureTranslateService(): void {
    const defaultLocale = 'en';

    this.translateService.addLangs([
      'en',
      'nl',
      'fr',
      'de',
      'es',
      'zh',
    ]);

    this.translateService.setDefaultLang(defaultLocale);
    this.translateService.use(this.userSettings.language || defaultLocale);
  }
}
