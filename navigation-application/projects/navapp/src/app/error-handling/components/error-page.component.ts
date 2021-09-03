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

import { Component, Input } from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { TranslateService } from '@ngx-translate/core';

import { MenuStateService } from '../../main-menu/services/menu-state.service';
import { NavigationService } from '../../services/navigation.service';
import { AppError } from '../models/app-error';
import { AppErrorCodes } from '../models/app-error-codes';
import { CriticalError } from '../models/critical-error';

@Component({
  selector: 'brna-error-page',
  templateUrl: 'error-page.component.html',
  styleUrls: ['error-page.component.scss'],
})
export class ErrorPageComponent {
  @Input()
  error: AppError;

  constructor(
    private readonly navigationService: NavigationService,
    private readonly menuStateService: MenuStateService,
    private readonly translateService: TranslateService,
  ) {}

  get errorMessage(): string {
    const message = this.error.message || this.translateService.instant('ERROR_PAGE_DEFAULT_ERROR');
    const code = this.error.code || 500;

    return this.translateService.instant('ERROR_PAGE_MESSAGE', { message, code });
  }
  get isGoToHomeButtonVisible(): boolean {
    return !(this.error instanceof CriticalError) && !!this.menuStateService.currentHomeMenuItem && !this.isReloadButtonVisible;
  }

  get isReloadButtonVisible(): boolean {
    return (this.error instanceof AppError) && this.error.code === AppErrorCodes.Timeout;
  }

  navigateToHome(): void {
    this.navigationService.navigateToHome(NavigationTrigger.NotDefined);
  }

  reloadPage(): void {
    this.navigationService.reload();
  }
}
