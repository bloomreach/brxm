/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('ChannelCtrl', () => {
  let $q;
  let $rootScope;
  let $timeout;
  let ChannelCtrl;
  let ChannelService;
  let SidePanelService;
  let ComponentsService;
  let FeedbackService;
  let HippoIframeService;
  let PageMetaDataService;
  let SessionService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

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

      SidePanelService = jasmine.createSpyObj('SidePanelService', [
        'open',
      ]);

      ComponentsService = jasmine.createSpyObj('ComponentsService', [
        'components',
        'getComponents',
      ]);
      ComponentsService.getComponents.and.returnValue(resolvedPromise);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'load',
        'reload',
      ]);

      PageMetaDataService = jasmine.createSpyObj('PageMetaDataService', [
        'getRenderVariant',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        $stateParams,
        ChannelService,
        SidePanelService,
        ComponentsService,
        HippoIframeService,
        PageMetaDataService,
      });
    });

    spyOn(FeedbackService, 'showError');
  });

  it('loads the initial page', () => {
    expect(HippoIframeService.load).toHaveBeenCalledWith('/testPath');
  });

  it('initializes controller', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.$onInit();
    expect(ChannelCtrl.hasPreviewConfiguration).toEqual(true);

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelCtrl.$onInit();
    expect(ChannelCtrl.hasPreviewConfiguration).toEqual(false);
  });

  it('checks with the session service is the current user has write access', () => {
    spyOn(SessionService, 'hasWriteAccess');

    ChannelCtrl.$onInit();

    SessionService.hasWriteAccess.and.returnValue(false);
    expect(ChannelCtrl.hasPreviewConfiguration).toBe(false);
    expect(ChannelCtrl.isEditable()).toBe(false);

    SessionService.hasWriteAccess.and.returnValue(true);
    ChannelCtrl.hasPreviewConfiguration = true;
    expect(ChannelCtrl.isEditable()).toBe(true);
  });

  it('gets the render variant from the page meta-data service', () => {
    PageMetaDataService.getRenderVariant.and.returnValue('variant1');
    expect(ChannelCtrl.getRenderVariant()).toBe('variant1');
  });

  it('should not be true by default (components overlay)', () => {
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
  });

  it('creates preview configuration when it has not been created yet before enabling edit mode', () => {
    const deferCreatePreview = $q.defer();
    const deferReload = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);
    HippoIframeService.reload.and.returnValue(deferReload.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.$onInit();

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();

    deferCreatePreview.resolve(); // preview creation completed successfully, reload page
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
    expect(HippoIframeService.reload).toHaveBeenCalled();

    deferReload.resolve(); // reload completed successfully, enter edit mode
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
  });

  it('shows an error when the creation of the preview configuration fails', () => {
    const deferCreatePreview = $q.defer();

    ChannelService.hasPreviewConfiguration.and.returnValue(false);
    ChannelService.createPreviewConfiguration.and.returnValue(deferCreatePreview.promise);

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    ChannelCtrl.$onInit();
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);

    expect(ChannelService.createPreviewConfiguration).toHaveBeenCalled();
    expect(ChannelCtrl.isCreatingPreview).toEqual(true);
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);

    deferCreatePreview.reject();
    $rootScope.$digest();

    expect(ChannelCtrl.isCreatingPreview).toEqual(false);
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ENTER_EDIT');
  });

  it('does not create preview configuration when it has already been created when enabling edit mode', () => {
    ChannelService.hasPreviewConfiguration.and.returnValue(true);
    ChannelCtrl.isComponentsOverlayDisplayed = true;
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

  it('opens the content editor in the right sidepanel when told so', () => {
    ChannelCtrl.editContent('testUuid');
    expect(SidePanelService.open).toHaveBeenCalledWith('right', 'testUuid');
  });
});
