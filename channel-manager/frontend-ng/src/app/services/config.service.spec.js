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

import angular from 'angular';
import 'angular-mocks';

describe('ConfigService', () => {
  let CmsServiceMock;
  let $window;
  let ConfigService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    CmsServiceMock = jasmine.createSpyObj('CmsServiceMock', [
      'getConfig',
      'publish',
      'subscribe',
    ]);

    CmsServiceMock.getConfig.and.returnValue({
      locale: 'nl',
      apiUrlPrefix: 'https://127.0.0.1:9080/web/one/two',
      contextPaths: ['/one', '/two'],
    });

    angular.mock.module(($provide) => {
      $provide.value('CmsService', CmsServiceMock);
    });

    inject((_$window_, _ConfigService_) => {
      $window = _$window_;
      ConfigService = _ConfigService_;
    });
  });

  it('allows custom configuration passed in by the CmsService', () => {
    expect(ConfigService.locale).toEqual('nl');
    expect(ConfigService.apiUrlPrefix).toEqual('https://127.0.0.1:9080/web/one/two');
    expect(ConfigService.contextPath).toEqual('/one');
  });

  it('sets the context path correctly', () => {
    ConfigService.setContextPathForChannel('testpath');
    expect(ConfigService.contextPath).toBe('testpath');
  });

  it('knows the CMS context path', () => {
    expect(ConfigService.getCmsContextPath()).toBe('/test/');
  });

  it('falls back to a default CMS context path', () => {
    delete $window.parent;
    expect(ConfigService.getCmsContextPath()).toBe('/cms/');
  });
});
