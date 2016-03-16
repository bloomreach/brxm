/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelCtrl', () => {
  'use strict';

  let ChannelService;
  let ComponentsService;
  let ScalingService;
  let ChannelCtrl;
  let $rootScope;
  let $q;

  beforeEach(() => {
    module('hippo-cm');

    inject(($controller, _$rootScope_, _$q_) => {
      const resolvedPromise = _$q_.when();

      $rootScope = _$rootScope_;
      $q = _$q_;

      ChannelService = jasmine.createSpyObj('ChannelService', [
        'getUrl',
        'hasPreviewConfiguration',
        'createPreviewConfiguration',
      ]);
      ChannelService.getUrl.and.returnValue('/test/url');
      ChannelService.createPreviewConfiguration.and.returnValue(resolvedPromise);

      ComponentsService = jasmine.createSpyObj('ComponentsService', [
        'components',
        'getComponents',
      ]);
      ComponentsService.getComponents.and.returnValue(resolvedPromise);

      ScalingService = jasmine.createSpyObj('ScalingService', [
        'init',
        'setPushWidth',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        ComponentsService,
        ChannelService,
        ScalingService,
      });
    });
  });

  it('resets the ScalingService pushWidth state during initialization', () => {
    ScalingService.setPushWidth.calls.reset();
    inject(($controller) => {
      $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        ScalingService,
      });
      expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
    });
  });

  it('gets the iframe URL from the channel service', () => {
    expect(ChannelCtrl.iframeUrl).toEqual('/test/url');
  });

  it('is not in edit mode by default', () => {
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('enables and disables edit mode when toggling it', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);

    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(true);
    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('creates preview configuration when it has not been created yet before enabling edit mode', () => {
    const deferCreatePreview = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.toggleEditMode();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditMode).toEqual(false);

    deferCreatePreview.resolve();
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isEditMode).toEqual(true);
  });

  it('does not create preview configuration when it has already been created when enabling edit mode', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.toggleEditMode();
    expect(ChannelService.createPreviewConfiguration).not.toHaveBeenCalled();
  });
});
