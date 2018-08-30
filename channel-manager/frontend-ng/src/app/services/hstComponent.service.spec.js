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

describe('HstComponentService', () => {
  let $q;
  let $rootScope;
  let $window;
  let ChannelService;
  let ConfigService;
  let HstComponentService;
  let HstService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$window_) => {
      $window = _$window_;
    });

    spyOn($window.APP_TO_CMS, 'publish').and.callThrough();
    spyOn($window.CMS_TO_APP, 'subscribe').and.callThrough();
  });

  beforeEach(() => {
    inject((_$q_, _$rootScope_, _ChannelService_, _ConfigService_, _HstService_, _HstComponentService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      HstService = _HstService_;
      HstComponentService = _HstComponentService_;
    });
  });

  describe('interaction with the CMS through the "path-picked" event', () => {
    it('subscribes to the CMS event "path-picked" upon construction', () => {
      expect($window.CMS_TO_APP.subscribe).toHaveBeenCalledWith('path-picked', jasmine.any(Function));
    });

    it('responds to callbacks with id "component-path-picker"', () => {
      const pathPickedHandler = spyOn(HstComponentService, 'pathPickedHandler');

      $window.CMS_TO_APP.publish('path-picked', 'random-id', '/path/picked');
      expect(pathPickedHandler).not.toHaveBeenCalled();

      $window.CMS_TO_APP.publish('path-picked', 'component-path-picker', '/path/picked');
      expect(pathPickedHandler).toHaveBeenCalledWith('/path/picked');
      expect(HstComponentService.pathPickedHandler).toBe(angular.noop);
    });

    it('resets the pathPickedHandler after a successful callback', () => {
      HstComponentService.pathPickedHandler = () => {};

      $window.CMS_TO_APP.publish('path-picked', 'random-id');
      expect(HstComponentService.pathPickedHandler).not.toBe(angular.noop);

      $window.CMS_TO_APP.publish('path-picked', 'component-path-picker');
      expect(HstComponentService.pathPickedHandler).toBe(angular.noop);
    });
  });

  describe('pickPath', () => {
    let pickPathPromise;

    beforeEach(() => {
      pickPathPromise = HstComponentService.pickPath('id', 'variant', 'name', 'value', 'pickerConfig', 'basePath');
    });

    it('publishes a "show-path-picker" event to the CMS application', () => {
      expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-path-picker', 'component-path-picker', 'value', 'pickerConfig');
    });

    it('sets the picked path when the pathPickedHandler is invoked', () => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());

      HstComponentService.pathPickedHandler('selected-path');
      expect(HstComponentService.setPathParameter).toHaveBeenCalledWith('id', 'variant', 'name', 'selected-path', 'basePath');
    });

    it('returns a promise that is resolved after the picked path is set', (done) => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());

      expect(pickPathPromise).toBeDefined();
      pickPathPromise.then(() => {
        done();
      });

      HstComponentService.pathPickedHandler('selected-path');
      $rootScope.$digest();
    });

    it('returns a promise that is rejected if setting the picked path fails', (done) => {
      const errorResponse = {};
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.reject(errorResponse));

      pickPathPromise.catch((response) => {
        expect(response).toEqual(errorResponse);
        done();
      });

      HstComponentService.pathPickedHandler('selected-path');
      $rootScope.$digest();
    });
  });

  describe('setPathParameter', () => {
    it('calls setParameter after parsing input', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', '/path');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');
    });

    it('turns a relative path into an absolute path', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', 'path');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');
    });

    it('passes a relative path if basePath is set and path is a part of basePath', () => {
      spyOn(HstComponentService, 'setParameter');

      HstComponentService.setPathParameter('a', 'b', 'c', '/path', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/path');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', '/root');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root/path', '/root');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', 'path');

      HstComponentService.setPathParameter('a', 'b', 'c', '/root/path', '/root/');
      expect(HstComponentService.setParameter).toHaveBeenCalledWith('a', 'b', 'c', 'path');
    });
  });

  describe('setParameter', () => {
    beforeEach(() => {
      spyOn(HstService, 'doPutForm');
      spyOn(ChannelService, 'recordOwnChange');
    });

    it('uses the HstService to store the parameter data of a component', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', 'variant', 'name', 'value');
      $rootScope.$digest();

      expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', 'variant');
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('does not record own change if parameter change fails', () => {
      HstService.doPutForm.and.returnValue($q.reject());

      HstComponentService.setParameter('id', 'variant', 'name', 'value');
      $rootScope.$digest();

      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('URI-encodes the variant name', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', '@variant', 'name', 'value');
      expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', '%40variant');
    });
  });

  describe('getProperties', () => {
    beforeEach(() => {
      spyOn(HstService, 'doGet').and.returnValue($q.resolve());
      ConfigService.locale = 'test-locale';
    });

    it('uses the HstService to get the properties of a component', () => {
      HstComponentService.getProperties('id', 'variant');
      expect(HstService.doGet).toHaveBeenCalledWith('id', 'variant', 'test-locale');
      $rootScope.$digest();
    });

    it('URI-encodes the variant name', () => {
      HstComponentService.getProperties('id', '@variant');
      expect(HstService.doGet).toHaveBeenCalledWith('id', '%40variant', 'test-locale');
    });
  });
});
