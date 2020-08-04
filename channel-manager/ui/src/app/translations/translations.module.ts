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

import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, NgModule } from '@angular/core';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';

import enTranslations from '../../assets/i18n/en.json';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../services/ng1/config.ng1.service';

import { Translations } from './models/translations.model';
import { translateHttpLoaderFactory } from './services/translate-http-loader.factory';

@NgModule({
  imports: [
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: translateHttpLoaderFactory,
        deps: [HttpClient, Location],
      },
    }),
  ],
  exports: [TranslateModule],
})
export class TranslationsModule {
  constructor(
    private readonly translateService: TranslateService,
    @Inject(NG1_CONFIG_SERVICE) ng1ConfigService: Ng1ConfigService,
  ) {
    this.setUpTranslations(ng1ConfigService.locale, 'en', enTranslations);
  }

  private setUpTranslations(language: string, defaultLanguage: string, defaultTranslations: Translations): void {
    this.setDefaultLanguage(defaultLanguage, defaultTranslations);

    this.translateService.addLangs([
      'en',
      'nl',
      'fr',
      'de',
      'es',
      'zh',
    ]);

    this.translateService.use(language || defaultLanguage);
  }

  private setDefaultLanguage(defaultLang: string, defaultTranslations: Translations): void {
    this.translateService.setTranslation(defaultLang, defaultTranslations);
    this.translateService.setDefaultLang(defaultLang);
  }
}
