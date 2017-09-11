/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('Hippo Global', () => {
  let HippoGlobal;

  const Hippo = {
    name: 'hippoGlobalObject',
    someMethod() {},
    someOtherMethod() {},
    someProperty: '',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm', ($provide) => {
      $provide.decorator('$window', ($delegate) => {
        Object.assign($delegate.parent, {
          Hippo,
        });

        return $delegate;
      });
    });

    angular.mock.inject((_HippoGlobal_) => {
      HippoGlobal = _HippoGlobal_;
    });
  });

  it('should reference the hippo global object', () => {
    expect(HippoGlobal).toEqual(Hippo);
  });
});

