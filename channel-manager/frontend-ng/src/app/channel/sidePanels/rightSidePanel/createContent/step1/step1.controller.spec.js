/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('Create content step 1 controller', () => {
  let $ctrl;
  let $q;
  let $rootScope;
  let Step1Service;
  let CreateContentService;
  let CmsService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContent.step1');

    inject((
      $controller,
      _$q_,
      _$rootScope_,
      _CmsService_,
      _CreateContentService_,
      _Step1Service_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
      CreateContentService = _CreateContentService_;
      Step1Service = _Step1Service_;

      $ctrl = $controller('step1Ctrl');
    });
    spyOn(CmsService, 'reportUsageStatistic');
  });

  it('gets values from the service', () => {
    Step1Service.defaultPath = 'test-defaultPath';
    Step1Service.documentType = 'test-documentType';
    Step1Service.documentTypes = 'test-documentTypes';
    Step1Service.name = 'test-name';
    Step1Service.rootPath = 'test-rootPath';
    Step1Service.url = 'test-url';
    Step1Service.locale = 'test-locale';
    Step1Service.defaultPickerPath = 'test-defaultPickerPath';

    expect($ctrl.defaultPath).toBe('test-defaultPath');
    expect($ctrl.documentType).toBe('test-documentType');
    expect($ctrl.documentTypes).toBe('test-documentTypes');
    expect($ctrl.locale).toBe('test-locale');
    expect($ctrl.name).toBe('test-name');
    expect($ctrl.rootPath).toBe('test-rootPath');
    expect($ctrl.defaultPickerPath).toBe('test-defaultPickerPath');
    expect($ctrl.url).toBe('test-url');
  });

  it('sets values on the service', () => {
    $ctrl.defaultPath = 'test-defaultPath';
    $ctrl.documentType = 'test-documentType';
    $ctrl.locale = 'test-locale';
    $ctrl.name = 'test-name';
    $ctrl.url = 'test-url';
    $ctrl.rootPath = 'test-rootPath';

    expect(Step1Service.defaultPath).toBe('test-defaultPath');
    expect(Step1Service.documentType).toBe('test-documentType');
    expect(Step1Service.name).toBe('test-name');
    expect(Step1Service.url).toBe('test-url');
    expect(Step1Service.rootPath).toBe('test-rootPath');
  });

  it('creates a draft and passes it on to the next step together with url and locale', () => {
    const document = { displayName: 'document-name' };
    Step1Service.url = 'test-url';
    Step1Service.locale = 'test-locale';
    spyOn(Step1Service, 'createDraft').and.returnValue($q.resolve(document));
    spyOn(CreateContentService, 'next');

    $ctrl.submit();
    $rootScope.$digest();

    expect(Step1Service.createDraft).toHaveBeenCalled();
    expect(CreateContentService.next).toHaveBeenCalledWith(document, 'test-url', 'test-locale');
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CreateContent1Create');
  });

  it('stops create content when close is called', () => {
    spyOn(CreateContentService, 'stop');
    $ctrl.close();
    expect(CreateContentService.stop).toHaveBeenCalled();
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CreateContent1Cancel');
  });
});
