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

import { NgxLoggerLevel } from 'ngx-logger';

import { CustomWindow } from '../shared/services/window-ref.service';

import { getConfigurationLogLevel } from './get-configuration-log-level';

describe('getConfigurationLogLevel', () => {
  let navappSettingsOldValue: any;

  beforeEach(() => {
    navappSettingsOldValue = (window as unknown as CustomWindow).NavAppSettings;

    (window as unknown as CustomWindow).NavAppSettings = {
      appSettings: {
        logLevel: 'DEBUG',
      },
    } as any;
  });

  afterEach(() => {
    (window as unknown as CustomWindow).NavAppSettings = navappSettingsOldValue;
  });

  it('should return the minimum log level', () => {
    const expected = NgxLoggerLevel.DEBUG;

    const actual = getConfigurationLogLevel();

    expect(actual).toBe(expected);
  });

  it('should return undefined if NavAppSettings is not defined', () => {
    (window as unknown as CustomWindow).NavAppSettings = undefined;

    const actual = getConfigurationLogLevel();

    expect(actual).toBe(undefined);
  });

  it('should return undefined if appSettings section is not defined', () => {
    (window as unknown as CustomWindow).NavAppSettings.appSettings = undefined;

    const actual = getConfigurationLogLevel();

    expect(actual).toBe(undefined);
  });

  it('should return undefined if unknown logLevel value is set', () => {
    (window as unknown as CustomWindow).NavAppSettings.appSettings.logLevel = 'UNKNOWN' as any;

    const actual = getConfigurationLogLevel();

    expect(actual).toBe(undefined);
  });
});
