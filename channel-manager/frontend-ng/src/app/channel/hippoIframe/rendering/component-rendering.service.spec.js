/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  let HippoIframeService;
  let PageStructureService;
  let SpaService;

  let mockComponent;
  let mockPage;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    SpaService = jasmine.createSpyObj('SpaService', ['isSpa', 'renderComponent']);

    angular.mock.module(($provide) => {
      $provide.value('SpaService', SpaService);
    });

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _ComponentRenderingService_,
      _HippoIframeService_,
      _PageStructureService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentRenderingService = _ComponentRenderingService_;
      HippoIframeService = _HippoIframeService_;
      PageStructureService = _PageStructureService_;
    });

    mockComponent = { id: '1234' };
    mockPage = {
      getComponentById: jasmine.createSpy('getComponentById').and.returnValue(mockComponent),
    };
    spyOn(PageStructureService, 'getPage').and.returnValue(mockPage);

    SpaService.renderComponent.and.returnValue(false);
  });

  describe('render component', () => {
    beforeEach(() => {
      spyOn(PageStructureService, 'renderComponent');
    });

    it('rejects unknown components', (done) => {
      mockPage.getComponentById.and.returnValue(null);
      spyOn($log, 'warn');

      ComponentRenderingService.renderComponent('1234')
        .then(() => fail('Should be rejected'))
        .catch((e) => {
          expect($log.warn).toHaveBeenCalledWith('Cannot render unknown component with ID \'1234\'.');
          expect(e.message).toBe('Cannot render unknown component with ID \'1234\'.');
          done();
        });

      $rootScope.$apply();
    });

    it('tries to render a component via the SPA', (done) => {
      SpaService.isSpa.and.returnValue(true);

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).toHaveBeenCalledWith(mockComponent, { foo: 1 });
          expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
          done();
        });

      $rootScope.$apply();
    });

    it('tries to render a component via the PageStructureService', (done) => {
      SpaService.isSpa.and.returnValue(false);
      PageStructureService.renderComponent.and.returnValue($q.resolve());

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).not.toHaveBeenCalled();
          expect(PageStructureService.renderComponent).toHaveBeenCalledWith(mockComponent, { foo: 1 });
          done();
        });

      $rootScope.$apply();
    });

    it('rejects when render component via the PageStructureService fails', (done) => {
      PageStructureService.renderComponent.and.returnValue($q.reject());

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .then(() => fail('Should be rejected'))
        .catch(done);

      $rootScope.$apply();
    });

    it('rejects when render component via the SPA fails ', (done) => {
      SpaService.isSpa.and.returnValue(true);
      SpaService.renderComponent.and.returnValue($q.reject('error'));
      spyOn($log, 'error');

      ComponentRenderingService.renderComponent('1234', { foo: 1 })
        .catch((error) => {
          expect(error).toBe('error');
          expect($log.error).toHaveBeenCalledWith('error');
        })
        .then(done);

      $rootScope.$apply();
    });

    it('reloads the iframe when render component via the PageStructureService fails', (done) => {
      PageStructureService.renderComponent.and.returnValue($q.reject());
      spyOn(HippoIframeService, 'reload');

      ComponentRenderingService.renderComponent('1234', { foo: 1 });
      $rootScope.$apply();

      expect(HippoIframeService.reload).toHaveBeenCalled();

      done();
    });
  });
});
