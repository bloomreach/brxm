/*!
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

import { NGXLogger } from 'ngx-logger';

import { UserSettings } from '../models/dto/user-settings.dto';

import { userSettingsFactory } from './user-settings.factory';

describe('userSettingsFactory', () => {
  let windowRefMock: any;
  let loggerMock: jasmine.SpyObj<NGXLogger>;

  beforeEach(() => {
    loggerMock = jasmine.createSpyObj('NGXLogger', [
      'error',
      'info',
    ]);
  });

  describe('when the global configuration object is not set', () => {
    beforeEach(() => {
      windowRefMock = {
        nativeWindow: {},
      };
    });

    it('should print an error ', () => {
      userSettingsFactory(windowRefMock, loggerMock);

      expect(loggerMock.error).toHaveBeenCalledWith('The global configuration object is not set');
    });

    it('should return an empty object', () => {
      const expected: UserSettings = {} as any;

      const actual = userSettingsFactory(windowRefMock, loggerMock);

      expect(actual).toEqual(expected);
    });
  });

  describe('when the global configuration object is set', () => {
    beforeEach(() => {
      windowRefMock = {
        nativeWindow: {
          NavAppSettings: {},
        },
      };
    });

    it('should log an error when the app settings are not set in the global object', () => {
      userSettingsFactory(windowRefMock, loggerMock);

      expect(loggerMock.error).toHaveBeenCalledWith('User settings part of the global configuration object is not set');
    });

    it('should return an empty object the app settings are not set in the global object', () => {
      const expected: UserSettings = {} as any;

      const actual = userSettingsFactory(windowRefMock, loggerMock);

      expect(actual).toEqual(expected);
    });

    describe('and the user settings object is set', () => {
      const userSettingsMock: UserSettings = {
        userName: 'Some name',
        email: 'some.email@domain.com',
        language: 'en-US',
        timeZone: 'Timezone',
      } as any;

      beforeEach(() => {
        windowRefMock.nativeWindow = {
          NavAppSettings: {
            userSettings: userSettingsMock,
          },
        };
      });

      it('should return the user settings object', () => {
        const expected = userSettingsMock;

        const actual = userSettingsFactory(windowRefMock, loggerMock);

        expect(actual).toEqual(expected);
      });

      it('should log user settings', () => {
        userSettingsFactory(windowRefMock, loggerMock);

        expect(loggerMock.info).toHaveBeenCalledWith('User settings', userSettingsMock);
      });
    });
  });
});
