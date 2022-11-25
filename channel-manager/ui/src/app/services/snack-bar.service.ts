/*
 * Copyright 2022 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SnackBarComponent } from '../shared/components/snackbar/snack-bar.component';
import { SnackBarConfig } from '../shared/models/snackbar.model';

@Injectable({
  providedIn: 'root',
})
export class SnackBarService {
  constructor(
    private readonly snackBar: MatSnackBar,
  ) { }

  open(message: string, config: SnackBarConfig = {}): void {
    const { dismiss, duration, warning } = config;

    this.snackBar.openFromComponent(SnackBarComponent, {
      data: {
        message,
        dismiss,
        warning,
      },
      duration: 'duration' in config ? duration : 4000,
    });
  }

  warning(message: string): void {
    const config: SnackBarConfig = {
      dismiss: true,
      duration: undefined,
      warning: true,
    };

    this.open(message, config);
  }
}
