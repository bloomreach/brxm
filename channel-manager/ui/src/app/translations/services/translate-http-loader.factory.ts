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
import { TranslateHttpLoader } from '@ngx-translate/http-loader';

import { getAntiCacheQueryParam } from './get-anti-cache-query-param';

export const translateHttpLoaderFactory = (http: HttpClient, location: Location): TranslateHttpLoader => {
  const locationPath = location.path();
  const antiCacheQueryParam = getAntiCacheQueryParam(locationPath);

  return new TranslateHttpLoader(http, 'i18n/', `.json?${antiCacheQueryParam}`);
};
