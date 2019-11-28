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

import { Location } from '@angular/common';
import { NGXLogger } from 'ngx-logger';

import { AppSettings } from '../models/dto/app-settings.dto';

import { appSettingsFactory } from './app-settings.factory';

describe('appSettingsFactory', () => {
  let windowRefMock: any;
  let locationMock: jasmine.SpyObj<Location>;
  let loggerMock: jasmine.SpyObj<NGXLogger>;

  beforeEach(() => {
    locationMock = jasmine.createSpyObj('Location', [
      'path',
    ]);

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

    it('should print an error', () => {
      appSettingsFactory(windowRefMock, locationMock, loggerMock);

      expect(loggerMock.error).toHaveBeenCalledWith('The global configuration object is not set');
    });

    it('should return an empty object', () => {
      const expected: AppSettings = {} as any;

      const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock);

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

    it('should print an error when the app settings are not set in the global object', () => {
      appSettingsFactory(windowRefMock, locationMock, loggerMock);

      expect(loggerMock.error).toHaveBeenCalledWith('App settings part of the global configuration object is not set');
    });

    it('should return an empty object the app settings are not set in the global object', () => {
      const expected: AppSettings = {} as any;

      const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock);

      expect(actual).toEqual(expected);
    });

    describe('and the app settings object is set', () => {
      const appSettingsMock: AppSettings = {
        basePath: '/base/path',
        iframesConnectionTimeout: 10000,
      } as any;

      beforeEach(() => {
        windowRefMock.nativeWindow = {
          NavAppSettings: {
            appSettings: appSettingsMock,
          },
        };
      });

      it('should return the app settings object', () => {
        const expected = appSettingsMock;

        const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock);

        expect(actual).toEqual(expected);
      });

      it('should log app settings', () => {
        appSettingsFactory(windowRefMock, locationMock, loggerMock);

        expect(loggerMock.info).toHaveBeenCalledWith('App settings', appSettingsMock);
      });

      describe('basePath', () => {
        it ('should be returned from the app settings object', () => {
          const expected = appSettingsMock.basePath;

          const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock).basePath;

          expect(actual).toEqual(expected);
        });

        describe('when it is not set in the app settings object', () => {
          beforeEach(() => {
            windowRefMock.nativeWindow = {
              NavAppSettings: {
                appSettings: { basePath: undefined },
              },
            };
          });

          it('should be read from the location', () => {
            const expected = '/base/path/from/browser/location';

            locationMock.path.and.returnValue(expected);

            const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock).basePath;

            expect(actual).toEqual(expected);
          });

          it('should strip off the query string', () => {
            const expected = '/base/path/from/browser/location';

            locationMock.path.and.returnValue('/base/path/from/browser/location?queryParam=value');

            const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock).basePath;

            expect(actual).toEqual(expected);
          });
        });
      });

      describe('iframesConnectionTimeout', () => {
        it ('should be returned from the app settings object', () => {
          const expected = appSettingsMock.iframesConnectionTimeout;

          const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock).iframesConnectionTimeout;

          expect(actual).toEqual(expected);
        });

        describe('when it is not set in the app settings object', () => {
          beforeEach(() => {
            windowRefMock.nativeWindow = {
              NavAppSettings: {
                appSettings: {
                  basePath: '/base/path',
                  iframesConnectionTimeout: undefined,
                },
              },
            };
          });

          it('should be set to the default value', () => {
            const expected = 30000;

            const actual = appSettingsFactory(windowRefMock, locationMock, loggerMock).iframesConnectionTimeout;

            expect(actual).toEqual(expected);
          });
        });
      });
    });
  });
});
