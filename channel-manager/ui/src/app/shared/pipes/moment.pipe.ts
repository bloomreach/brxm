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

import { Inject, Pipe, PipeTransform } from '@angular/core';
import moment from 'moment-timezone';

import { Ng1ConfigService, NG1_CONFIG_SERVICE } from '../../services/ng1/config.ng1.service';

const DEFAULT_FORMAT_PATTERN = 'LLL';

@Pipe({name: 'moment'})
export class MomentPipe implements PipeTransform {
  constructor(@Inject(NG1_CONFIG_SERVICE) private readonly ng1ConfigService: Ng1ConfigService) {
    moment.tz.setDefault(this.ng1ConfigService.timeZone);
    moment.locale(this.ng1ConfigService.locale || 'en');
  }

  transform(value: number | string | Date, format?: string): string {
    return moment(value).format(format || DEFAULT_FORMAT_PATTERN);
  }
}
