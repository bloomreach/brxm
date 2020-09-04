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

import { Ng1ConfigService } from '../../services/ng1/config.ng1.service';

import { MomentPipe } from './moment.pipe';

describe('MomentPipe', () => {
  let pipe: MomentPipe;

  beforeEach(() => {
    const configService = {
      timeZone: 'UTC',
      locale: 'en',
    } as Ng1ConfigService;

    pipe = new MomentPipe(configService);
  });

  it('should format date with the default pattern', () => {
    const date = new Date(Date.UTC(2019, 0, 20, 10, 30, 20));

    const formattedDate = pipe.transform(date);

    expect(formattedDate).toBe('January 20, 2019 10:30 AM');
  });

  it('should format date with the provided pattern', () => {
    const date = new Date(Date.UTC(2000, 10, 18, 21, 43, 40));

    const formattedDate = pipe.transform(date, 'L LTS');

    expect(formattedDate).toBe('11/18/2000 9:43:40 PM');
  });
});
