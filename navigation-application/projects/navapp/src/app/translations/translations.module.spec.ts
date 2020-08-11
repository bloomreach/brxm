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

import { TranslateService } from '@ngx-translate/core';

import enTranslations from '../../navapp-assets/i18n/en.json';
import { UserSettings } from '../models/dto/user-settings.dto';
import { UserSettingsMock } from '../models/dto/user-settings.mock';

import { TranslationsModule } from './translations.module';

describe('TranslationsModule', () => {
  let module: TranslationsModule;
  let translateServiceMock: jasmine.SpyObj<TranslateService>;
  let userSettingsMock: UserSettings;

  beforeEach(() => {
    translateServiceMock = jasmine.createSpyObj('TranslateService', [
      'setTranslation',
      'setDefaultLang',
      'addLangs',
      'use',
    ]);

    userSettingsMock = new UserSettingsMock();
    userSettingsMock.language = 'nl';

    module = new TranslationsModule(translateServiceMock, userSettingsMock);
  });

  it('should set a default language', () => {
    expect(translateServiceMock.setDefaultLang).toHaveBeenCalledWith('en');
  });

  it('should set default translations', () => {
    expect(translateServiceMock.setTranslation).toHaveBeenCalledWith('en', enTranslations);
  });

  it('should set default translations before the default language', () => {
    expect(translateServiceMock.setTranslation).toHaveBeenCalledBefore(translateServiceMock.setDefaultLang);
  });

  it('should add available languages', () => {
    expect(translateServiceMock.addLangs).toHaveBeenCalledWith([
      'en',
      'nl',
      'fr',
      'de',
      'es',
      'zh',
    ]);
  });

  it('should use the language from user settings', () => {
    expect(translateServiceMock.use).toHaveBeenCalledWith('nl');
  });

  describe('when the current language is not set in user settings', () => {
    beforeEach(() => {
      translateServiceMock = jasmine.createSpyObj('TranslateService', [
        'setTranslation',
        'setDefaultLang',
        'addLangs',
        'use',
      ]);

      userSettingsMock = new UserSettingsMock();
      userSettingsMock.language = undefined;

      module = new TranslationsModule(translateServiceMock, userSettingsMock);
    });

    it('should use the default language', () => {
      expect(translateServiceMock.use).toHaveBeenCalledWith('en');
    });
  });
});
