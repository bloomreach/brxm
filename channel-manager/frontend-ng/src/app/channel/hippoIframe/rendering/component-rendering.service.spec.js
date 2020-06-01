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

describe('ComponentRenderingService', () => {
  let $log;
  let $q;
  let $rootScope;
  let ComponentRenderingService;
  let PageStructureService;
  let SpaService;

  let component;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _ComponentRenderingService_,
      _PageStructureService_,
      _SpaService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentRenderingService = _ComponentRenderingService_;
      PageStructureService = _PageStructureService_;
      SpaService = _SpaService_;
    });

    component = { id: '1234' };
    spyOn(PageStructureService, 'getComponentById').and.returnValue(component);
  });

  describe('render component', () => {
    beforeEach(() => {
      spyOn(SpaService, 'renderComponent');
      spyOn(PageStructureService, 'renderComponent');
    });

    it('rejects unknown components', (done) => {
      PageStructureService.getComponentById.and.returnValue(null);
      spyOn($log, 'warn');

      ComponentRenderingService.renderComponent('1234')
        .then(() => fail('Should be rejected'))
        .catch(() => {
          expect($log.warn).toHaveBeenCalledWith('Cannot render unknown component with ID \'1234\'');
          done();
        });

      $rootScope.$apply();
    });

    it('first tries to render a component via the SPA', (done) => {
      SpaService.renderComponent.and.returnValue(true);

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).toHaveBeenCalledWith(component, { foo: 1 });
          expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
          done();
        });

      $rootScope.$apply();
    });

    it('second renders a component via the PageStructureService', (done) => {
      SpaService.renderComponent.and.returnValue(false);
      PageStructureService.renderComponent.and.returnValue($q.resolve());

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).toHaveBeenCalledWith(component, { foo: 1 });
          expect(PageStructureService.renderComponent).toHaveBeenCalledWith(component, { foo: 1 });
          done();
        });

      $rootScope.$apply();
    });

    it('rejects when render component via the PageStructureService fails', (done) => {
      SpaService.renderComponent.and.returnValue(false);
      PageStructureService.renderComponent.and.returnValue($q.reject());

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => fail('Should be rejected'))
        .catch(done);

      $rootScope.$apply();
    });
  });
});
