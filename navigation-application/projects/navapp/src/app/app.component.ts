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

import { Component, HostBinding, Inject, OnInit, ViewChild } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';

import { MatSidenav } from '@angular/material';
import { Observable } from 'rxjs';

import { AppError } from './error-handling/models/app-error';
import { ErrorHandlingService } from './error-handling/services/error-handling.service';
import { UserSettings } from './models/dto/user-settings.dto';
import { OverlayService } from './services/overlay.service';
import { USER_SETTINGS } from './services/user-settings';
import { RightSidePanelService } from './top-panel/services/right-side-panel.service';

@Component({
  selector: 'brna-root',
  templateUrl: './app.component.html',
  styleUrls: ['app.component.scss'],
})
export class AppComponent implements OnInit {
  @HostBinding('class.mat-typography')
  typography = true;

  @ViewChild(MatSidenav)
  sidenav: MatSidenav;

  constructor(
    private translateService: TranslateService,
    private overlayService: OverlayService,
    private rightSidePanelService: RightSidePanelService,
    private errorHandlingService: ErrorHandlingService,
    @Inject(USER_SETTINGS) private userSettings: UserSettings,
  ) {}

  get isOverlayVisible$(): Observable<boolean> {
    return this.overlayService.visible$;
  }

  get error(): AppError {
    return this.errorHandlingService.currentError;
  }

  ngOnInit(): void {
    this.configureTranslateService();
    this.rightSidePanelService.setSidenav(this.sidenav);
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
