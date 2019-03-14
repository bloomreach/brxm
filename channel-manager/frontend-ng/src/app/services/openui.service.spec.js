/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import angular from 'angular';
import 'angular-mocks';

describe('OpenUIService', () => {
  let $q;
  let $rootScope;
  let iframe;
  let OpenUIService;
  let Penpal;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    iframe = angular.element('<iframe src="about:blank"></iframe>');

    inject((_$q_, _$rootScope_, _OpenUIService_, _Penpal_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      OpenUIService = _OpenUIService_;
      Penpal = _Penpal_;
    });
  });

  it('connects to the child', () => {
    const params = {};

    spyOn(Penpal, 'connectToChild').and.returnValue({
      iframe,
      promise: $q.resolve('child'),
    });

    OpenUIService.connect(params);
    $rootScope.$digest();

    expect(iframe).toHaveAttr(
      'sandbox',
      'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts',
    );
    expect(Penpal.connectToChild).toHaveBeenCalledWith(params);
  });
});
