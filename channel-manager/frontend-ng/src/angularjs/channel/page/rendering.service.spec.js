/*
 *
 *  * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

import angular from 'angular';
import 'angular-mocks';

describe('RenderingService', () => {
  let RenderingService;
  let $httpBackend;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.page');

    inject((_RenderingService_, _$httpBackend_) => {
      $httpBackend = _$httpBackend_;
      RenderingService = _RenderingService_;
    });
  });


  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('fetches a component markup', () => {
    const component = jasmine.createSpyObj('component', ['getRenderUrl']);

    component.getRenderUrl.and.returnValue('/test-render-url');
    $httpBackend.whenPOST('/test-render-url').respond('<div>component markup</div>');

    RenderingService.fetchComponentMarkup(component, { foo: 1, bar: 'a:b' });

    $httpBackend.expectPOST('/test-render-url', 'foo=1&bar=a%3Ab', {
      Accept: 'text/html',
      'Content-Type': 'application/x-www-form-urlencoded',
    });
    $httpBackend.flush();
  });

  it('fetches a container markup', () => {
    const container = jasmine.createSpyObj('container', ['getRenderUrl']);

    container.getRenderUrl.and.returnValue('/test-render-url');
    $httpBackend.whenGET('/test-render-url').respond('<div>container markup</div>');

    RenderingService.fetchContainerMarkup(container);

    $httpBackend.expectGET('/test-render-url');
    $httpBackend.flush();
  });
});
