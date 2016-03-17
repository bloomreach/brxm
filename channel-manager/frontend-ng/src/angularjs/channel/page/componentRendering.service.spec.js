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
  let RenderingService;
  let $log;
  let $q;

  beforeEach(() => {
    module('hippo-cm.channel.page');

    inject((_$httpBackend_, _$q_, _$log_, _PageStructureService_, _RenderingService_) => {
      $log = _$log_;
      $q = _$q_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
    });
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/page/componentRendering.service.fixture.html');
  });

  it('renders a component', () => {
    const component = jasmine.createSpyObj('component', ['getJQueryElement']);
    const iframeElement = $j('#component');

    component.getJQueryElement.and.returnValue(iframeElement);
    spyOn(PageStructureService, 'getComponent').and.returnValue(component);
    spyOn(PageStructureService, 'updateComponent');
    spyOn(RenderingService, 'fetchComponentMarkup').and.returnValue($q.when('{ data: <div>component markup</div> }'));

    window.CMS_TO_APP.publish('render-component', '1234', { foo: 1, bar: 'a:b' });

    expect(RenderingService.fetchComponentMarkup).toHaveBeenCalledWith(component, { foo: 1, bar: 'a:b' });
    expect(PageStructureService.replaceComponent).toHaveBeenCalledWith(component, '<div>component markup</div>');
  });

  it('logs a warning when the component to render cannot be found', () => {
    spyOn(PageStructureService, 'getComponent').and.returnValue(null);
    spyOn($log, 'warn');

    window.CMS_TO_APP.publish('render-component', 'unknown-component-id', {});

    expect($log.warn).toHaveBeenCalled();
  });
});
