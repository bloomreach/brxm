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

import { AppError } from '../error-handling/models/app-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';

import { BootstrapService } from './bootstrap.service';

let bootstrapResolve: () => void;
let bootstrapReject: () => void;
export const appBootstrappedPromise = new Promise((resolve, reject) => {
  bootstrapResolve = resolve;
  bootstrapReject = reject;
});

export const scheduleAppBootstrapping = (
  bootstrapService: BootstrapService,
  busyIndicatorService: BusyIndicatorService,
  errorHandlingService: ErrorHandlingService,
) => {
  busyIndicatorService.show();

  bootstrapService.bootstrap()
    .then(
      bootstrapResolve,
      error => {
        if (error instanceof AppError) {
          errorHandlingService.setError(error);
        } else {
          errorHandlingService.setInternalError(
            'An error occurred during initialization',
            error ? error.toString() : undefined,
          );
        }
      },
    )
    .then(() => busyIndicatorService.hide());
};
