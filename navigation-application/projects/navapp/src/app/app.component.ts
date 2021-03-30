/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { Component, HostBinding, Inject, OnInit, ViewChild } from '@angular/core';
import { MatSidenav } from '@angular/material/sidenav';

import { AppError } from './error-handling/models/app-error';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { AppSettings } from './models/dto/app-settings.dto';
import { UserSettings } from './models/dto/user-settings.dto';
import { APP_SETTINGS } from './services/app-settings';
import { MainLoaderService } from './services/main-loader.service';
import { OverlayService } from './services/overlay.service';
import { PageTitleManagerService } from './services/page-title-manager.service';
import { PENDO } from './services/pendo';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

@Component({
  selector: 'brna-root',
  templateUrl: 'app.component.html',
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
export class AppComponent implements OnInit {
  @HostBinding('class.mat-typography')
  typography = true;

  @ViewChild(MatSidenav, { static: true })
  sidenav: MatSidenav;

  constructor(
    private readonly overlayService: OverlayService,
    private readonly rightSidePanelService: RightSidePanelService,
    private readonly errorHandlingService: ErrorHandlingService,
    private readonly mainLoaderService: MainLoaderService,
    private readonly pageTitleManagerService: PageTitleManagerService,
    @Inject(PENDO) private readonly pendo: pendo.Pendo,
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    @Inject(USER_SETTINGS) private readonly userSettings: UserSettings,
  ) { }

  get error(): AppError {
    return this.errorHandlingService.currentError;
  }

  get isOverlayVisible(): boolean {
    return this.overlayService.isVisible;
  }

  get isLoaderVisible(): boolean {
    return this.mainLoaderService.isVisible;
  }

  ngOnInit(): void {
    this.rightSidePanelService.setSidenav(this.sidenav);

    this.pageTitleManagerService.init();
    this.setupPendo();
  }

  private setupPendo(): void {
    if (!this.appSettings.usageStatisticsEnabled) {
      return;
    }

    this.pendo.initialize({
      visitor: {
        id: this.userSettings.email || this.userSettings.userName,
      },
      account: {
        id: this.userSettings.accountId,
      },
    });
  }
}
