/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { NavItem as NavItemDto } from '@bloomreach/navapp-communication';
import { isObservable, Subject, Subscription } from 'rxjs';

import { NavItem } from './nav-item.model';

describe('NavItem', () => {
  let model: NavItem;
  let unsubscribe: Subject<void>;

  const dto: NavItemDto = {
    id: 'some-id',
    appIframeUrl: 'https://some-url',
    appPath: 'some/path',
    displayName: 'some display name',
  };

  beforeEach(() => {
    unsubscribe = new Subject();
    model = new NavItem(dto);
  });

  it('should return an id', () => {
    const expected = 'some-id';

    const actual = model.id;

    expect(actual).toBe(expected);
  });

  it('should return an application iframe url', () => {
    const expected = 'https://some-url';

    const actual = model.appIframeUrl;

    expect(actual).toBe(expected);
  });

  it('should return an application path', () => {
    const expected = 'some/path';

    const actual = model.appPath;

    expect(actual).toBe(expected);
  });

  it('should return a displayName', () => {
    const expected = 'some display name';

    const actual = model.displayName;

    expect(actual).toBe(expected);
  });
});
