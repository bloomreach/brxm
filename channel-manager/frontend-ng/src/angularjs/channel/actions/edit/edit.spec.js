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
  let ChannelService;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$translate_, _ChannelService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
    });

    spyOn($translate, 'instant');
    spyOn(ChannelService, 'getName').and.returnValue('test-name');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $element = angular.element('<channel-edit on-done="onDone()"></channel-edit>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channel-edit');
  }

  it('initializes correctly', () => {
    compileDirectiveAndGetController();

    expect(ChannelService.getName).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_CHANNEL_EDIT_TITLE', { channelName: 'test-name' });
  });

  it('notifies the event "on-done" when clicking the back button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();

    expect($scope.onDone).toHaveBeenCalled();
  });

  it('notifies the event "on-done" when clicking the save button', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-save').click();

    expect($scope.onDone).toHaveBeenCalled();
  });
});
