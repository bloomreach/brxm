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

import { TranslateService } from '@ngx-translate/core';
import 'jest-extended';

import enTranslations from '../../assets/i18n/en.json';
import { Ng1ConfigService } from '../services/ng1/config.service';

import { TranslationsModule } from './translations.module';

describe('TranslationsModule', () => {
  let module: TranslationsModule;
  let translateServiceMock: { [key: string]: jest.Mock };
  let settings: Ng1ConfigService;

  beforeEach(() => {
    translateServiceMock = {
      setTranslation: jest.fn(),
      setDefaultLang: jest.fn(),
      addLangs: jest.fn(),
      use: jest.fn(),
    };

    settings = {
      locale: 'nl',
    } as unknown as typeof settings;

    module = new TranslationsModule(translateServiceMock as unknown as TranslateService, settings);
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

  it('should use the language from the settings', () => {
    expect(translateServiceMock.use).toHaveBeenCalledWith('nl');
  });

  describe('when the current language is not set in the settings', () => {
    beforeEach(() => {
      settings = { locale: null } as unknown as typeof settings;

      module = new TranslationsModule(translateServiceMock as unknown as TranslateService, settings);
    });

    it('should use the default language', () => {
      expect(translateServiceMock.use).toHaveBeenCalledWith('en');
    });
  });
});
