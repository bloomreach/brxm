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

    model = new NavItem(dto, unsubscribe);
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

  it('should not be active', () => {
    const actual = model.active;

    expect(actual).toBeFalsy();
  });

  it('should return an activation observable', () => {
    const actual = isObservable(model.active$);

    expect(actual).toBeTruthy();
  });

  it('should be be able to be activated', () => {
    model.activate();

    const actual = model.active;

    expect(actual).toBeTruthy();
  });

  describe('activation observable', () => {
    let currentState: boolean;
    let subscription: Subscription;

    beforeEach(() => {
      currentState = undefined;
      subscription = model.active$.subscribe(x => currentState = x);
    });

    afterEach(() => {
      subscription.unsubscribe();
    });

    it('should emit false as a first value', () => {
      expect(currentState).toBeFalsy();
    });

    it('should emit true when the nav item is activated', () => {
      model.activate();

      expect(currentState).toBeTruthy();
    });

    it('should not emit values after unsubscribe emitted', () => {
      unsubscribe.next();

      model.activate();

      expect(currentState).toBeFalsy();
    });
  });
});
