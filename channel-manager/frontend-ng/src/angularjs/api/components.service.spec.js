/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
