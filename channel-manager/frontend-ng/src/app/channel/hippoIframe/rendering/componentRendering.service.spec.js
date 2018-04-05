/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('ComponentRenderingService', () => {
  let $window;
  let ComponentRenderingService;
  let PageStructureService;
  let SpaService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.page');

    inject((_$window_, _ComponentRenderingService_, _PageStructureService_, _SpaService_) => {
      $window = _$window_;
      ComponentRenderingService = _ComponentRenderingService_;
      PageStructureService = _PageStructureService_;
      SpaService = _SpaService_;
    });
  });

  it('handles the render-component event from ExtJS', () => {
    spyOn(ComponentRenderingService, 'renderComponent');
    $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1, bar: 'a:b' });
    expect(ComponentRenderingService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1, bar: 'a:b' });
  });

  describe('renderComponent', () => {
    beforeEach(() => {
      spyOn(SpaService, 'renderComponent');
      spyOn(PageStructureService, 'renderComponent');
    });

    it('first tries to render a component via the SPA', () => {
      SpaService.renderComponent.and.returnValue(true);

      ComponentRenderingService.renderComponent('1234', { foo: 1 });

      expect(SpaService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
      expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
    });

    it('second renders a component via the PageStructureService', () => {
      SpaService.renderComponent.and.returnValue(false);

      ComponentRenderingService.renderComponent('1234', { foo: 1 });

      expect(SpaService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
      expect(PageStructureService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
    });
  });
});
