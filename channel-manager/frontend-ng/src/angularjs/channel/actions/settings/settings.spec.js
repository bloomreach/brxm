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

import angular from 'angular';
import 'angular-mocks';

describe('ChannelSettings', () => {
  let $scope;
  let $rootScope;
  let $compile;
  let $translate;
  let $element;
  let $q;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let ConfigService;
  let channelInfoDescription;
  const channel = {
    properties: {
      textField: 'summer',
      dropDown: 'large',
      boolean: true,
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$q_, _$translate_, _ChannelService_, _FeedbackService_, _HippoIframeService_,
            _ConfigService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      $q = _$q_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      ConfigService = _ConfigService_;
    });

    channelInfoDescription = {
      fieldGroups: [
        { value: ['textField', 'dropDown', 'boolean'],
          titleKey: 'group1',
        },
      ],
      propertyDefinitions: {
        textField: {
          isRequired: false,
          defaultValue: '',
          name: 'textField',
          valueType: 'STRING',
          annotations: [],
        },
        dropDown: {
          isRequired: false,
          defaultValue: '',
          name: 'dropDown fallback',
          valueType: 'STRING',
          annotations: [
            {
              type: 'DropDownList',
              value: ['small', 'medium', 'large'],
            },
          ],
        },
        boolean: {
          isRequired: false,
          defaultValue: false,
          name: 'boolean fallback',
          valueType: 'BOOLEAN',
          annotations: [],
        },
      },
      i18nResources: {
        textField: 'Text Field',
        dropDown: 'Drop Down',
        boolean: 'Boolean',
        group1: 'Field Group 1',
      },
    };

    spyOn($translate, 'instant').and.callFake(key => key);
    spyOn(ChannelService, 'getName').and.returnValue('test-name');
    spyOn(ChannelService, 'reload').and.returnValue($q.when(channel));
    spyOn(ChannelService, 'getChannel').and.returnValue(channel);
    spyOn(ChannelService, 'getChannelInfoDescription').and.returnValue($q.when(channelInfoDescription));
    spyOn(FeedbackService, 'showErrorOnSubpage');
    spyOn(FeedbackService, 'showErrorResponseOnSubpage');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $scope.onError = jasmine.createSpy('onError');
    $scope.onSuccess = jasmine.createSpy('onSuccess');

    $element = angular.element(`
      <channel-settings on-done="onDone()" on-success="onSuccess(key, params)" on-error="onError(key, params)">
      </channel-settings>
    `);
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channelSettings');
  }

  it('initializes correctly when fetching channel setting from backend is successful', () => {
    compileDirectiveAndGetController();

    expect(ChannelService.reload).toHaveBeenCalled();
    expect(ChannelService.getName).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_CHANNEL_SETTINGS_TITLE', { channelName: 'test-name' });

    expect($element.find('.qa-fieldgroup').text()).toBe('Field Group 1');
    expect($element.find('.qa-field-textField label').text()).toBe('Text Field');
    expect($element.find('.qa-field-dropDown label').text()).toBe('Drop Down');
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

  it('marks required fields as dirty on save', () => {
    channelInfoDescription.propertyDefinitions.dropDown.isRequired = true;
    channelInfoDescription.editable = true;
    const ChannelSettingsCtrl = compileDirectiveAndGetController();
    expect(ChannelSettingsCtrl.form.dropDown.$dirty).toEqual(false);

    $element.find('.qa-action').click();

    expect(ChannelSettingsCtrl.form.dropDown.$dirty).toEqual(true);
  });

  it('does not save settings when a required field is invalid', () => {
    channelInfoDescription.propertyDefinitions.dropDown.isRequired = true;
    spyOn(ChannelService, 'saveChannel');
    compileDirectiveAndGetController();

    $element.find('.qa-action').click();

    expect(ChannelService.saveChannel).not.toHaveBeenCalled();
  });

  it('notifies the event "on-success" when saving is successful', () => {
    channelInfoDescription.editable = true;
    spyOn(ChannelService, 'saveChannel').and.returnValue($q.when());
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(HippoIframeService, 'reload');
    compileDirectiveAndGetController();

    $element.find('.qa-action').click();

    expect(ChannelService.saveChannel).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onSuccess).toHaveBeenCalledWith('CHANNEL_PROPERTIES_SAVE_SUCCESS', undefined);
  });

  it('shows feedback message when saving is failed', () => {
    channelInfoDescription.editable = true;
    spyOn(ChannelService, 'saveChannel').and.returnValue($q.reject());
    compileDirectiveAndGetController();

    $element.find('.qa-action').click();

    expect(ChannelService.saveChannel).toHaveBeenCalled();
    expect(FeedbackService.showErrorResponseOnSubpage).toHaveBeenCalledWith(undefined, 'ERROR_CHANNEL_PROPERTIES_SAVE_FAILED');
  });

  it('applies a fall-back strategy when determining a field label', () => {
    const ChannelSettingsCtrl = compileDirectiveAndGetController();
    expect(ChannelSettingsCtrl.getLabel('textField')).toBe('Text Field');

    delete channelInfoDescription.i18nResources.textField;
    expect(ChannelSettingsCtrl.getLabel('textField')).toBe('textField');
  });

  it('displays an alert message when the current channel is locked', () => {
    ConfigService.cmsUser = 'admin';
    channelInfoDescription.lockedBy = 'tester';
    channelInfoDescription.editable = true;
    compileDirectiveAndGetController();
    expect(FeedbackService.showErrorOnSubpage).toHaveBeenCalledWith('ERROR_CHANNEL_SETTINGS_READONLY', { lockedBy: 'tester' });

    FeedbackService.showErrorOnSubpage.calls.reset();
    channelInfoDescription.lockedBy = 'admin';
    compileDirectiveAndGetController();
    expect(FeedbackService.showErrorOnSubpage).not.toHaveBeenCalled();

    FeedbackService.showErrorOnSubpage.calls.reset();
    delete channelInfoDescription.lockedBy;
    compileDirectiveAndGetController();
    expect(FeedbackService.showErrorOnSubpage).not.toHaveBeenCalled();
  });
});
