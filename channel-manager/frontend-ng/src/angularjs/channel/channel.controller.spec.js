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
  let ScalingService;
  let FeedbackService;
  let PageMetaDataService;
  let SessionService;
  let ChannelCtrl;
  let HippoIframeService;
  let $rootScope;
  let $timeout;
  let $q;

  beforeEach(() => {
    module('hippo-cm');

    inject(($controller, _$rootScope_, _$timeout_, _$q_, _FeedbackService_, _SessionService_) => {
      const resolvedPromise = _$q_.when();

      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      $q = _$q_;
      FeedbackService = _FeedbackService_;
      SessionService = _SessionService_;

      const $stateParams = {
        initialRenderPath: '/testPath',
      };

      ChannelService = jasmine.createSpyObj('ChannelService', [
        'hasPreviewConfiguration',
        'createPreviewConfiguration',
        'getChannel',
        'publishChanges',
        'discardChanges',
        'getCatalog',
        'getSiteMapId',
      ]);

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
        'sync',
      ]);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'load',
        'reload',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        $stateParams,
        ComponentsService,
        ChannelService,
        PageMetaDataService,
        ScalingService,
        HippoIframeService,
      });
    });

    spyOn(FeedbackService, 'showError');
  });

  it('loads the initial page', () => {
    expect(HippoIframeService.load).toHaveBeenCalledWith('/testPath');
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

  it('checks with the session service is the current user has write access', () => {
    spyOn(SessionService, 'hasWriteAccess');

    SessionService.hasWriteAccess.and.returnValue(true);
    expect(ChannelCtrl.isEditable()).toBe(true);
    SessionService.hasWriteAccess.and.returnValue(false);
    expect(ChannelCtrl.isEditable()).toBe(false);
  });

  it('gets the render variant from the page meta-data service', () => {
    PageMetaDataService.getRenderVariant.and.returnValue('variant1');
    expect(ChannelCtrl.getRenderVariant()).toBe('variant1');
  });

  it('is not in edit mode by default', () => {
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('enables and disables edit mode when toggling it', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);

    ChannelCtrl.enableEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(true);
    ChannelCtrl.disableEditMode();
    expect(ChannelCtrl.isEditMode).toEqual(false);
  });

  it('creates preview configuration when it has not been created yet before enabling edit mode', () => {
    const deferCreatePreview = $q.defer();
    const deferReload = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);
    HippoIframeService.reload.and.returnValue(deferReload.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.enableEditMode();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditMode).toEqual(false);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();

    deferCreatePreview.resolve(); // preview creation completed successfully, reload page
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditMode).toEqual(false);
    expect(HippoIframeService.reload).toHaveBeenCalled();

    deferReload.resolve(); // reload completed successfully, enter edit mode
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isEditMode).toEqual(true);
  });

  it('shows an error when the creation of the preview configuration fails', () => {
    const deferCreatePreview = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.enableEditMode();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isEditMode).toEqual(false);

    deferCreatePreview.reject();
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isEditMode).toEqual(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ENTER_EDIT');
  });

  it('does not create preview configuration when it has already been created when enabling edit mode', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.enableEditMode();
    expect(ChannelService.createPreviewConfiguration).not.toHaveBeenCalled();
  });

  it('correctly shows and hides subpages', () => {
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    HippoIframeService.reload.and.returnValue($q.when());

    ChannelCtrl.showSubpage('test');
    expect(ChannelCtrl.isSubpageOpen()).toBe(true);

    ChannelCtrl.hideSubpage();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    $timeout.flush();
    expect(ScalingService.sync).toHaveBeenCalled();

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageError('key', { param: 'value' });
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('key', { param: 'value' });

    FeedbackService.showError.calls.reset();
    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageError();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).not.toHaveBeenCalled();

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageSuccess('key', { param: 'value' });
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageSuccess();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
  });

  it('opens the menu editor when told so', () => {
    ChannelCtrl.editMenu('testUuid');

    expect(ChannelCtrl.menuUuid).toBe('testUuid');
    expect(ChannelCtrl.currentSubpage).toBe('menu-editor');
  });
});
