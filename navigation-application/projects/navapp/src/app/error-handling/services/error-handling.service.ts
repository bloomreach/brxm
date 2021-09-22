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

import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ClientErrorCodes } from '@bloomreach/navapp-communication';
import { TranslateService } from '@ngx-translate/core';
import { NGXLogger } from 'ngx-logger';

import { ConnectionService } from '../../services/connection.service';
import { AppError } from '../models/app-error';
import { CriticalError } from '../models/critical-error';
import { InternalError } from '../models/internal-error';
import { NotFoundError } from '../models/not-found-error';
import { TimeoutError } from '../models/timeout-error';

@Injectable()
export class ErrorHandlingService {
  private appError: AppError;

  constructor(
    private readonly connectionService: ConnectionService,
    private readonly snackBar: MatSnackBar,
    private readonly translateService: TranslateService,
    private readonly logger: NGXLogger,
  ) {
    this.connectionService.onError$.subscribe(({errorCode, message, errorType}) => {
      this.setClientError(errorCode, message, errorType);
    });
  }

  get currentError(): AppError {
    return this.appError;
  }

  private set error(value: AppError) {
    if (value) {
      this.translateError(value);

      this.logger.error(
        `Code: "${value.code}"`,
        `Message: "${value.message}"`,
        `Public description: "${value.description}"`,
        `Description: "${value.internalDescription}"`,
      );
    }

    this.appError = value;
  }

  setError(error: AppError): void {
    this.error = error;
  }

  setCriticalError(message: string, internalDescription?: string): void {
    this.error = new CriticalError(message, internalDescription);
  }

  setNotFoundError(publicDescription?: string, internalDescription?: string): void {
    this.error = new NotFoundError(publicDescription, internalDescription);
  }

  setInternalError(publicDescription?: string, internalDescription?: string): void {
    this.error = new InternalError(publicDescription, internalDescription);
  }

  setTimeoutError(publicDescription?: string, internalDescription?: string): void {
    this.error = new TimeoutError(publicDescription, internalDescription);
  }

  setClientError(errorCode: ClientErrorCodes, message?: string, errorType?: string): void {
    const errorCodeAsText = this.translateService.instant(this.mapClientErrorCodeToText(errorCode));

    if (errorType === 'lenient') {
      const errorMessage = message
        ? this.translateService.instant('ERROR_SNACK_BAR_MESSAGE', {
          error: errorCodeAsText,
          cause: message,
        })
        : errorCodeAsText;

      this.snackBar.open(errorMessage, this.translateService.instant('ERROR_SNACK_BAR_DISMISS'), {
        duration: 5 * 1000,
        horizontalPosition: 'right',
        verticalPosition: 'top',
      });
    } else {
      this.error = new AppError(
        this.mapClientErrorCodeToHttpErrorCode(errorCode),
        errorCodeAsText,
        message,
      );
    }
  }

  clearError(): void {
    this.appError = undefined;
  }

  private mapClientErrorCodeToHttpErrorCode(code: ClientErrorCodes): number {
    switch (code) {
      case ClientErrorCodes.NotAuthorizedError:
        return 403;

      case ClientErrorCodes.PageNotFoundError:
        return 404;

      default:
        return 500;
    }
  }

  private mapClientErrorCodeToText(code: ClientErrorCodes): string {
    switch (code) {
      case ClientErrorCodes.NotAuthorizedError:
        return 'Not authorized';

      case ClientErrorCodes.PageNotFoundError:
        return 'Page is not found';

      default:
        return 'Something went wrong';
    }
  }

  private translateError(error: AppError): void {
    if (error.message) {
      error.message = this.translateService.instant(error.message);
    }

    if (error.description) {
      error.description = this.translateService.instant(error.description);
    }
  }
}
