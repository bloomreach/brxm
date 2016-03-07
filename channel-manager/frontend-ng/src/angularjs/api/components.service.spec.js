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

describe('ComponentsService', function () {
  'use strict';

  var $q;
  var $rootScope;
  var MountServiceMock;
  var ComponentsService;

  beforeEach(function () {
    module('hippo-cm-api');

    MountServiceMock = jasmine.createSpyObj('MountService', [
      'getComponentsToolkit',
    ]);

    module(function ($provide) {
      $provide.value('MountService', MountServiceMock);
    });

    inject(function (_$q_, _$rootScope_, _ComponentsService_) {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentsService = _ComponentsService_;
    });
  });

  it('returns list of components in alphabetical order', function () {
    var mockComponents = [
      {
        label: 'foo component',
      },
      {
        label: 'bah component',
      },
      {
        label: 'foo2 component',
      },
    ];
    var deferred = $q.defer();

    MountServiceMock.getComponentsToolkit.and.returnValue(deferred.promise);

    ComponentsService.getComponents().then(function (components) {
      expect(components.map(function (c) {
        return c.label;
      }))
        .toEqual(['bah component', 'foo component', 'foo2 component']);
    });
    deferred.resolve(mockComponents);
    $rootScope.$digest();
  });
});
