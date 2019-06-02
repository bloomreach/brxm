/*
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

import Penpal from 'penpal';

import { connectToParent, createProxies } from './parent';

describe('connectToParent', () => {
  beforeEach(() => {
    spyOn(Penpal, 'connectToParent').and.callThrough();
  });

  it('should pass config to penpal connectToParent', () => {
    const parentOrigin = 'about:blank';
    const methods = {
      logout: () => {},
    };

    const config = {
      parentOrigin,
      methods,
    };

    connectToParent(config);
    expect(Penpal.connectToParent).toHaveBeenCalledWith(config);
  });

  it('should default the config methods to an empty object', () => {
    const parentOrigin = 'about:blank';
    const config = {
      parentOrigin,
    };

    connectToParent(config);
    expect(Penpal.connectToParent).toHaveBeenCalledWith({
      parentOrigin,
      methods: {},
    });
  });

  it('should proxy methods', () => {
    const methods = {
      navigate: jasmine.createSpy('navigate'),
      getNavItems: jasmine.createSpy('getNavItems'),
    };
    const proxies = createProxies(methods);

    proxies.getNavItems();
    proxies.navigate({ path: 'test' });

    expect(proxies.navigate).not.toBe(methods.navigate);

    expect(methods.navigate).toHaveBeenCalled();
    expect(methods.getNavItems).toHaveBeenCalled();
  });

  it('should pass proxied methods if available', () => {
    const parentOrigin = 'about:blank';
    const methods = {
      navigate: () => {},
    };
    const config = {
      parentOrigin,
      methods,
    };

    connectToParent(config);
    expect(Penpal.connectToParent).toHaveBeenCalled();
    expect(Penpal.connectToParent).not.toHaveBeenCalledWith(config); // So therefore the proxy is called.
  });
});
