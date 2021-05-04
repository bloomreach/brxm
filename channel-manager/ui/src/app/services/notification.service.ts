/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  constructor(
    private readonly snackBar: MatSnackBar,
    private readonly translateService: TranslateService,
  ) { }

  showNotification(message: string): void {
    const translatedMessage = this.translateService.instant(message);
    this.snackBar.open(translatedMessage, this.translateService.instant('DISMISS'), {
      duration: 5000,
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }

  showErrorNotification(message: string): void {
    const translatedMessage = this.translateService.instant(message);
    this.snackBar.open(translatedMessage, this.translateService.instant('DISMISS'), {
      horizontalPosition: 'end',
      verticalPosition: 'top',
    });
  }
}
