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

/* eslint-disable prefer-const */

describe('ChannelCtrl', () => {
  'use strict';

  let ViewAsCtrl;
  let $q;
  let $controller;
  let $rootScope;
  let SessionService;
  let HstService;
  let HippoIframeService;
  let FeedbackService;
  const MockConfigService = {
    variantsUuid: 'testVariantsUuid',
    locale: 'testLocale',
  };

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$controller_, _$rootScope_, _SessionService_, _HstService_, _HippoIframeService_,
            _FeedbackService_) => {
      $q = _$q_;
      $controller = _$controller_;
      $rootScope = _$rootScope_;
      SessionService = _SessionService_;
      HstService = _HstService_;
      HippoIframeService = _HippoIframeService_;
      FeedbackService = _FeedbackService_;

      spyOn(HstService, 'doGetWithParams');
      spyOn(HstService, 'doPost');
      spyOn(SessionService, 'registerInitCallback');
      spyOn(SessionService, 'unregisterInitCallback');
      spyOn(HippoIframeService, 'reload');
      spyOn(FeedbackService, 'showError');
    });
  });

  it('doesn\'t attempt to retrieve the global variants if the corresponding uuid is not present', () => {
    const scope = $rootScope.$new();
    spyOn(scope, '$on');

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: scope,
      ConfigService: {},
    });

    expect(HstService.doGetWithParams).not.toHaveBeenCalled();
    expect(SessionService.registerInitCallback.calls.mostRecent().args[0]).toBe('reloadGlobalVariants');
    expect(scope.$on.calls.mostRecent().args[0]).toBe('$destroy');
    expect(SessionService.unregisterInitCallback).not.toHaveBeenCalled();

    const destroyCallback = scope.$on.calls.mostRecent().args[1];
    destroyCallback();
    expect(SessionService.unregisterInitCallback).toHaveBeenCalledWith('reloadGlobalVariants');
  });

  it('sets the global variant if the selection changes', () => {
    const scope = $rootScope.$new();
    spyOn(scope, '$watch');

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: scope,
      ConfigService: { rootUuid: 'rootUuid' },
    });

    expect(scope.$watch.calls.mostRecent().args[0]).toBe('viewAs.selectedVariant');
    const variantChangedCallback = scope.$watch.calls.mostRecent().args[1];

    // ignore the initial call
    variantChangedCallback({ id: 'id' }, undefined);
    expect(HstService.doPost).not.toHaveBeenCalled();

    // do nothing when keeping the same id
    variantChangedCallback({ id: 'id' }, { id: 'id' });
    expect(HstService.doPost).not.toHaveBeenCalled();

    // talk to backend when ID changes
    HstService.doPost.and.returnValue($q.when());
    variantChangedCallback({ id: 'id2' }, { id: 'id1' });
    expect(HstService.doPost.calls.mostRecent().args).toEqual([null, 'rootUuid', 'setvariant', 'id2']);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();
    $rootScope.$digest();
    expect(HippoIframeService.reload).toHaveBeenCalled();

    // flash a toast if the backend returns an error
    HstService.doPost.and.returnValue($q.reject());
    variantChangedCallback({ id: 'id2', name: 'name2' }, { id: 'id1' });
    $rootScope.$digest();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_RELEVANCE_VARIANT_SELECTION_FAILED', { variant: 'name2' });
  });

  it('has no global variants if retrieving them fails', () => {
    HstService.doGetWithParams.and.returnValue($q.reject());

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
      ConfigService: MockConfigService,
    });

    expect(HstService.doGetWithParams).toHaveBeenCalledWith('testVariantsUuid', { locale: 'testLocale' }, 'globalvariants');

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_RELEVANCE_GLOBAL_VARIANTS_UNAVAILABLE');
    expect(ViewAsCtrl.globalVariants).toEqual([]);
    expect(ViewAsCtrl.selectedVariant).toBeUndefined();
  });

  it('selects the rendered variant', () => {
    const globalVariants = [
      { id: 'id1', name: 'name1' },
      { id: 'id2', name: 'name2', group: 'group2' },
    ];
    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants }));

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
      ConfigService: MockConfigService,
    });
    ViewAsCtrl.renderVariant = 'id2';
    $rootScope.$digest();

    expect(ViewAsCtrl.globalVariants).toBe(globalVariants);
    expect(ViewAsCtrl.selectedVariant).toBe(globalVariants[1]);
  });

  it('selects the first variant as a fallback when the render variant set but no longer available', () => {
    const globalVariants = [
      { id: 'id1', name: 'name1' },
      { id: 'id2', name: 'name2', group: 'group2' },
    ];
    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants }));

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
      ConfigService: MockConfigService,
    });
    ViewAsCtrl.renderVariant = 'id3';
    $rootScope.$digest();

    expect(ViewAsCtrl.globalVariants).toBe(globalVariants);
    expect(ViewAsCtrl.selectedVariant).toBe(globalVariants[0]);
  });

  it('does not fall back to the first variant when the render variant has not been set yet', () => {
    const globalVariants = [
      { id: 'id1', name: 'name1' },
      { id: 'id2', name: 'name2', group: 'group2' },
    ];
    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants }));

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
      ConfigService: MockConfigService,
    });
    $rootScope.$digest();

    expect(ViewAsCtrl.globalVariants).toBe(globalVariants);
    expect(ViewAsCtrl.selectedVariant).not.toBeDefined();
  });

  it('preserves the selection by ID', () => {
    const globalVariants1 = [
      { id: 'id1', name: 'name1' },
      { id: 'id2', name: 'name2', group: 'group2' },
    ];
    const globalVariants2 = [
      { id: 'id2', name: 'name2', group: 'group2' },
      { id: 'id3', name: 'name3' },
      { id: 'id1', name: 'name1', group: 'group1' },
    ];
    const globalVariants3 = [
      { id: 'id2', name: 'name2', group: 'group2' },
    ];
    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants1 }));

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
      ConfigService: MockConfigService,
    });
    ViewAsCtrl.renderVariant = 'id1';
    $rootScope.$digest();

    const reloadGlobalVariantsCallback = SessionService.registerInitCallback.calls.mostRecent().args[1];
    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants2 }));
    HstService.doGetWithParams.calls.reset();
    reloadGlobalVariantsCallback();
    expect(HstService.doGetWithParams).toHaveBeenCalled();
    $rootScope.$digest();

    expect(ViewAsCtrl.globalVariants).toBe(globalVariants2);
    expect(ViewAsCtrl.selectedVariant).toBe(globalVariants2[2]);

    HstService.doGetWithParams.and.returnValue($q.when({ data: globalVariants3 }));
    reloadGlobalVariantsCallback();
    $rootScope.$digest();

    expect(ViewAsCtrl.globalVariants).toBe(globalVariants3);
    expect(ViewAsCtrl.selectedVariant).toBe(globalVariants3[0]);
  });

  it('formats the display name of a global variant', () => {
    const variant1 = { name: 'name-only' };
    const variant2 = { name: 'name', group: 'group' };

    ViewAsCtrl = $controller('ViewAsCtrl', {
      $scope: $rootScope.$new(),
    });

    expect(ViewAsCtrl.makeDisplayName(variant1)).toBe('name-only');
    expect(ViewAsCtrl.makeDisplayName(variant2)).toBe('nameTOOLBAR_VIEW_AS_INFIXgroup');
  });
});
