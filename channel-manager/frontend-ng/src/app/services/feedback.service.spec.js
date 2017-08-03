/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

describe('FeedbackService', () => {
  let $log;
  let $translate;
  let $mdToast;
  let FeedbackService;
  let toast;

  const message = 'Test toast message';

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$log_, _$translate_, _$mdToast_, _FeedbackService_) => {
      $log = _$log_;
      $translate = _$translate_;
      $mdToast = _$mdToast_;
      FeedbackService = _FeedbackService_;
    });

    toast = jasmine.createSpyObj('toast', ['textContent', 'position', 'hideDelay', 'parent']);
    toast.textContent.and.returnValue(toast);
    toast.position.and.returnValue(toast);
    toast.hideDelay.and.returnValue(toast);
    toast.parent.and.returnValue(toast);

    spyOn($log, 'info');
    spyOn($translate, 'instant').and.returnValue(message);
    spyOn($mdToast, 'simple').and.returnValue(toast);
    spyOn($mdToast, 'show');
  });

  it('flashes a toast of the translated message', () => {
    const key = { trans: 'parent' };
    const params = { trans: 'tarent, too' };
    FeedbackService.showError(key, params);

    expect($translate.instant).toHaveBeenCalledWith(key, params);
    expect($mdToast.simple).toHaveBeenCalled();
    expect(toast.textContent).toHaveBeenCalledWith(message);
    expect(toast.position).toHaveBeenCalledWith('top right');
    expect(toast.hideDelay).toHaveBeenCalledWith(3000);
    expect(toast.parent).not.toHaveBeenCalled();
  });

  it('handles undefined error responses', () => {
    FeedbackService.showErrorResponse(undefined, 'defaultKey');
    expect($log.info).not.toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', undefined);

    FeedbackService.showErrorResponse(undefined, 'defaultKey');
    expect($log.info).not.toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', undefined);
  });

  it('handles null error responses', () => {
    FeedbackService.showErrorResponse(null, 'defaultKey');

    expect($log.info).not.toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', undefined);
  });

  it('logs messages at info level', () => {
    let response = { message: 'test log message' };
    FeedbackService.showErrorResponse(response, 'defaultKey');
    expect($log.info).toHaveBeenCalledWith('test log message');

    response = { parameterMap: { errorReason: 'another message' } };
    FeedbackService.showErrorResponse(response, 'defaultKey');
    expect($log.info).toHaveBeenCalledWith('another message');
  });

  it('maps ExtResponse error codes using the error map', () => {
    const map = { a: 'A' };
    const response = { errorCode: 'a' };
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('A', undefined);

    $translate.instant.calls.reset();
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('A', undefined);

    const params = { trans: 'parent' };
    response.data = params;
    response.errorCode = 'c';
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', params);

    $translate.instant.calls.reset();
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', params);
  });

  it('maps ErrorStatus error codes using the error map', () => {
    const map = { a: 'A' };
    const response = {
      error: 'a',
    };
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('A', undefined);

    const params = { trans: 'parent' };
    response.parameterMap = params;
    response.error = 'c';
    $translate.instant.calls.reset();
    FeedbackService.showErrorResponse(response, 'defaultKey', map);
    expect($translate.instant).toHaveBeenCalledWith('defaultKey', params);
  });

  it('bypasses translation when provided with a userMessage', () => {
    const response = { data: { userMessage: 'Message intended for {{subs}}', subs: 'Tester' } };
    FeedbackService.showErrorResponse(response, 'defaultKey');
    expect($translate.instant).not.toHaveBeenCalled();
    expect(toast.textContent).toHaveBeenCalledWith('Message intended for Tester');
  });
});
