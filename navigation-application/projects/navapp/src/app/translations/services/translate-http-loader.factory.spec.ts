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

import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

import { translateHttpLoaderFactory } from './translate-http-loader.factory';

describe('translateHttpLoaderFactory', () => {
  const httpClientMock: HttpClient = {} as any;
  const locationMock = jasmine.createSpyObj('Location', [
    'path',
  ]);

  const now = Date.now();

  beforeEach(() => {
    spyOn(window.Date, 'now').and.returnValue(now);
  });

  it('should create TranslateHttpLoader', () => {
    const expected = new TranslateHttpLoader(httpClientMock, 'navapp-assets/i18n/', `.json?antiCache=${now}`);

    const actual = translateHttpLoaderFactory(httpClientMock, locationMock);

    expect(actual).toEqual(expected);
  });
});
