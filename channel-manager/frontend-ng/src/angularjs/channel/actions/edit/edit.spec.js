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

describe('ChannelActionEdit', () => {
  'use strict';

  let $scope;
  let $rootScope;
  let $compile;
  let $translate;
  let $element;
  let $q;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  const channelInfoDescription = {};

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$q_, _$translate_, _ChannelService_, _FeedbackService_, _HippoIframeService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      $q = _$q_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
    });

    channelInfoDescription.fieldGroups = [
      { value: ['field1', 'field2'],
        titleKey: 'group1',
      },
    ];
    channelInfoDescription.i18nResources = {
      field1: 'Field 1',
      field2: 'Field 2',
      group1: 'Field Group 1',
    };

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getName').and.returnValue('test-name');
    spyOn(ChannelService, 'getChannelInfoDescription').and.returnValue($q.when(channelInfoDescription));
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $scope.onError = jasmine.createSpy('onError');
    $scope.onSuccess = jasmine.createSpy('onSuccess');

    $element = angular.element(`
      <channel-edit on-done="onDone()" on-success="onSuccess(key, params)" on-error="onError(key, params)">
      </channel-edit>
    `);
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channel-edit');
  }

  it('initializes correctly when fetching channel setting from backend is successful', () => {
    compileDirectiveAndGetController();

    expect(ChannelService.getName).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_CHANNEL_EDIT_TITLE', { channelName: 'test-name' });

    expect($element.find('.qa-channel-edit-save').is(':disabled')).toBe(true);
    expect($element.find('.qa-channel-edit-fieldgroup').text()).toBe('Field Group 1');
    expect($element.find('.qa-channel-edit-field label:eq(0)').text()).toBe('Field 1');
    expect($element.find('.qa-channel-edit-field label:eq(1)').text()).toBe('Field 2');
  });

  it('enables "save" button when form is dirty', () => {
    compileDirectiveAndGetController();

    $scope.form.$setDirty();
    $scope.$digest();

    expect($element.find('.qa-channel-edit-save').is(':enabled')).toBe(true);
  });

  it('notifies the event "on-error" when fetching channel setting from backend is failed', () => {
    ChannelService.getChannelInfoDescription.and.returnValue($q.reject());
    compileDirectiveAndGetController();

    expect($scope.onError).toHaveBeenCalledWith('ERROR_CHANNEL_INFO_RETRIEVAL_FAILED', undefined);
  });

  it('notifies the event "on-done" when clicking the back button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('notifies the event "on-success" when saving is successful', () => {
    spyOn(ChannelService, 'saveProperties').and.returnValue($q.when());
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when());
    compileDirectiveAndGetController();

    $scope.form.$setDirty();
    $scope.$digest();
    $element.find('.qa-channel-edit-save').click();

    expect(ChannelService.saveProperties).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onSuccess).toHaveBeenCalledWith('CHANNEL_PROPERTIES_SAVE_SUCCESS', undefined);
  });

  it('shows feedback message when saving is failed', () => {
    spyOn(ChannelService, 'saveProperties').and.returnValue($q.reject());
    spyOn(FeedbackService, 'showError');
    compileDirectiveAndGetController();
    const feedbackParent = $element.find('.feedback-parent');

    $scope.form.$setDirty();
    $scope.$digest();
    $element.find('.qa-channel-edit-save').click();

    expect(ChannelService.saveProperties).toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANNEL_PROPERTIES_SAVE_FAILED', undefined, feedbackParent);
  });
});
