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

describe('ChannelCtrl', function () {
  'use strict';

  var ChannelService;
  var ComponentsService;
  var ScalingService;
  var ChannelCtrl;
  var $rootScope;
  var $q;

  beforeEach(function () {
    module('hippo-cm');

    inject(function ($controller, _$rootScope_, _$q_) {
      var resolvedPromise = _$q_.when();

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
        'updatePushWidth',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        ComponentsService: ComponentsService,
        ChannelService: ChannelService,
        ScalingService: ScalingService,
      });
    });
  });

  it('gets the iframe URL from the channel service', function () {
    expect(ChannelCtrl.iframeUrl).toEqual('/test/url');
  });

  it('is not in edit mode by default', function () {
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('enables and disables edit mode when toggling it', function () {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);

    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(true);
    ChannelCtrl.toggleEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('creates preview configuration when it has not been created yet before enabling edit mode', function () {
    var deferCreatePreview = $q.defer();

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

  it('does not create preview configuration when it has already been created when enabling edit mode', function () {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.toggleEditMode();
    expect(ChannelService.createPreviewConfiguration).not.toHaveBeenCalled();
  });

});
