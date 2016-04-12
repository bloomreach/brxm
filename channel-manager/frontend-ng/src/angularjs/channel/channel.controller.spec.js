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

/* eslint-disable prefer-const */

describe('ChannelCtrl', () => {
  'use strict';

  let ChannelService;
  let ComponentsService;
  let DialogService;
  let ScalingService;
  let ConfigService;
  let FeedbackService;
  let PageMetaDataService;
  let SessionService;
  let ChannelCtrl;
  let HippoIframeService;
  let $rootScope;
  let $q;

  beforeEach(() => {
    module('hippo-cm');

    inject(($controller, _$rootScope_, _$q_, _ConfigService_, _DialogService_, _FeedbackService_, _SessionService_) => {
      const resolvedPromise = _$q_.when();

      $rootScope = _$rootScope_;
      $q = _$q_;
      ConfigService = _ConfigService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      SessionService = _SessionService_;

      ChannelService = jasmine.createSpyObj('ChannelService', [
        'getUrl',
        'hasPreviewConfiguration',
        'createPreviewConfiguration',
        'getChannel',
        'publishOwnChanges',
        'discardOwnChanges',
        'getCatalog',
      ]);
      ChannelService.getUrl.and.returnValue('/test/url');
      ChannelService.createPreviewConfiguration.and.returnValue(resolvedPromise);

      ComponentsService = jasmine.createSpyObj('ComponentsService', [
        'components',
        'getComponents',
      ]);
      ComponentsService.getComponents.and.returnValue(resolvedPromise);

      PageMetaDataService = jasmine.createSpyObj('PageMetaDataService', [
        'getRenderVariant',
      ]);

      ScalingService = jasmine.createSpyObj('ScalingService', [
        'init',
        'setPushWidth',
        'setViewPortWidth',
      ]);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'reload',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        ComponentsService,
        ChannelService,
        PageMetaDataService,
        ScalingService,
        HippoIframeService,
      });
    });
  });

  it('resets the ScalingService pushWidth state during initialization', () => {
    ScalingService.setPushWidth.calls.reset();
    ScalingService.setViewPortWidth.calls.reset();
    inject(($controller) => {
      $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        ScalingService,
      });
      expect(ScalingService.setPushWidth).toHaveBeenCalledWith(0);
      expect(ScalingService.setViewPortWidth).toHaveBeenCalledWith(0);
      expect(ChannelCtrl.isViewPortSelected(ChannelCtrl.viewPorts[0])).toBe(true);
    });
  });

  it('gets the iframe URL from the channel service', () => {
    expect(ChannelCtrl.iframeUrl).toEqual('/test/url');
  });

  it('checks with the session service is the current user has write access', () => {
    spyOn(SessionService, 'hasWriteAccess');

    SessionService.hasWriteAccess.and.returnValue(true);
    expect(ChannelCtrl.isEditable()).toBe(true);
    SessionService.hasWriteAccess.and.returnValue(false);
    expect(ChannelCtrl.isEditable()).toBe(false);
  });

  it('gets the component catalog from the channel service', () => {
    ChannelService.getCatalog.and.returnValue('dummy');
    expect(ChannelCtrl.getCatalog()).toBe('dummy');
  });

  it('gets the render variant from the page meta-data service', () => {
    PageMetaDataService.getRenderVariant.and.returnValue('variant1');
    expect(ChannelCtrl.getRenderVariant()).toBe('variant1');
  });

  it('is not in edit mode by default', () => {
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);
  });

  it('enables and disables edit mode when toggling it', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);

    ChannelCtrl.enterEditMode();
    expect(ChannelCtrl.isEditModeActive()).toEqual(true);
    ChannelCtrl.leaveEditMode();
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);
  });

  it('creates preview configuration when it has not been created yet before enabling edit mode', () => {
    const deferCreatePreview = $q.defer();
    const deferReload = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);
    HippoIframeService.reload.and.returnValue(deferReload.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.enterEditMode();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();

    deferCreatePreview.resolve(); // preview creation completed successfully, reload page
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);
    expect(HippoIframeService.reload).toHaveBeenCalled();

    deferReload.resolve(); // reload completed successfully, enter edit mode
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isEditModeActive()).toEqual(true);
  });

  it('shows an error when the creation of the preview configuration fails', () => {
    spyOn(FeedbackService, 'showError');
    const deferCreatePreview = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.enterEditMode();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);

    deferCreatePreview.reject();
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isEditModeActive()).toEqual(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ENTER_EDIT');
  });

  it('does not create preview configuration when it has already been created when enabling edit mode', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.enterEditMode();
    expect(ChannelService.createPreviewConfiguration).not.toHaveBeenCalled();
  });

  it('detects that the current user has pending changes', () => {
    ConfigService.cmsUser = 'testUser';
    ChannelService.getChannel.and.returnValue({ changedBySet: ['tobi', 'testUser', 'obiwan'] });

    expect(ChannelCtrl.hasChanges()).toBe(true);
  });

  it('detects that the current user has no pending changes', () => {
    ConfigService.cmsUser = 'testUser';
    ChannelService.getChannel.and.returnValue({ changedBySet: ['tobi', 'obiwan'] });

    expect(ChannelCtrl.hasChanges()).toBe(false);
  });

  it('publishes changes', () => {
    ChannelService.publishOwnChanges.and.returnValue($q.resolve());

    ChannelCtrl.publish();
    $rootScope.$digest();

    expect(ChannelService.publishOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('discards changes', () => {
    ChannelService.discardOwnChanges.and.returnValue($q.resolve());
    spyOn(DialogService, 'show').and.returnValue($q.resolve());
    spyOn(DialogService, 'confirm').and.callThrough();

    ChannelCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('does not discard changes if not confirmed', () => {
    spyOn(DialogService, 'show').and.returnValue($q.reject());
    spyOn(DialogService, 'confirm').and.callThrough();

    ChannelCtrl.discard();
    $rootScope.$digest();

    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(ChannelService.discardOwnChanges).not.toHaveBeenCalled();
  });

  it('shows the components sidenav button only in edit mode and if there are catalog components', () => {
    expect(ChannelCtrl.isEditModeActive()).toBe(false);
    expect(ChannelCtrl.showComponentsButton()).toBe(false);

    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.enterEditMode();

    ChannelService.getCatalog.and.returnValue(['dummy']);
    expect(ChannelCtrl.showComponentsButton()).toBe(true);

    ChannelService.getCatalog.and.returnValue([]);
    expect(ChannelCtrl.showComponentsButton()).toBe(false);
  });
});
