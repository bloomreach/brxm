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

import { NgModule } from '@angular/core';
import { LoggerModule, NGXLogger, NgxLoggerLevel } from 'ngx-logger';

import { getConfigurationLogLevel } from './get-configuration-log-level';

@NgModule({
  imports: [LoggerModule.forRoot(undefined)],
  exports: [LoggerModule],
})
export class ConfiguredLoggerModule {
  constructor(logger: NGXLogger) {
    logger.updateConfig({
      level: getConfigurationLogLevel() || NgxLoggerLevel.WARN,
      enableSourceMaps: true,
    });
  }
}
