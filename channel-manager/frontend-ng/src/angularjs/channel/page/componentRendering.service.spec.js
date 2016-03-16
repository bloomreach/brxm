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

describe('ComponentRenderingService', () => {
  'use strict';

  let PageStructureService;
  let $httpBackend;
  let $log;

  beforeEach(() => {
    module('hippo-cm.channel.page');

    inject((_$httpBackend_, _$log_, _PageStructureService_) => {
      $httpBackend = _$httpBackend_;
      $log = _$log_;
      PageStructureService = _PageStructureService_;
    });
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/page/componentRendering.service.fixture.html');
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('renders a component', () => {
    const component = jasmine.createSpyObj('component', ['getRenderUrl', 'getJQueryElement']);
    const iframeElement = $j('#component');

    component.getRenderUrl.and.returnValue('/test-render-url');
    component.getJQueryElement.and.returnValue(iframeElement);
    spyOn(PageStructureService, 'getComponent').and.returnValue(component);
    $httpBackend.whenPOST('/test-render-url').respond('<div>component markup</div>');

    window.CMS_TO_APP.publish('render-component', '1234', { foo: 1, bar: 'a:b' });

    $httpBackend.expectPOST('/test-render-url', 'foo=1&bar=a%3Ab', {
      Accept: 'text/html, */*',
      'Content-Type': 'application/x-www-form-urlencoded',
    });
    $httpBackend.flush();

    expect(component.getJQueryElement).toHaveBeenCalledWith('iframe');
    expect(iframeElement.html()).toEqual('<div>component markup</div>');
  });

  it('logs a warning when the component to render cannot be found', () => {
    spyOn(PageStructureService, 'getComponent').and.returnValue(null);
    spyOn($log, 'warn');

    window.CMS_TO_APP.publish('render-component', 'unknown-component-id', {});

    expect($log.warn).toHaveBeenCalled();
  });
});
