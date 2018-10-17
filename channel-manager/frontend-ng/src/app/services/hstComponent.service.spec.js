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
  let ChannelService;
  let ConfigService;
  let HstComponentService;
  let HstService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');
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

    it('uses the HstService to store the parameter data of a component', (done) => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', 'variant', 'name', 'value')
        .then(() => {
          expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', 'variant');
          expect(ChannelService.recordOwnChange).toHaveBeenCalled();
          done();
        });
      $rootScope.$digest();
    });

    it('does not record own change if parameter change fails', (done) => {
      HstService.doPutForm.and.returnValue($q.reject());

      HstComponentService.setParameter('id', 'variant', 'name', 'value')
        .catch(() => {
          expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
          done();
        });
      $rootScope.$digest();
    });

    it('URI-encodes the variant name', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameter('id', '@variant', 'name', 'value');
      expect(HstService.doPutForm).toHaveBeenCalledWith({ name: 'value' }, 'id', '%40variant');
    });
  });

  describe('setParameters', () => {
    beforeEach(() => {
      spyOn(HstService, 'doPutForm');
      spyOn(ChannelService, 'recordOwnChange');
    });

    it('uses the HstService to store the parameter data of a component', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameters('id', 'variant', { param1: 1, param2: 2 });
      $rootScope.$digest();

      expect(HstService.doPutForm).toHaveBeenCalledWith({ param1: 1, param2: 2 }, 'id', 'variant');
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('does not record own change if parameter change fails', () => {
      HstService.doPutForm.and.returnValue($q.reject());

      HstComponentService.setParameters('id', 'variant', { param1: 1, param2: 2 });
      $rootScope.$digest();

      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('URI-encodes the variant name', () => {
      HstService.doPutForm.and.returnValue($q.resolve());

      HstComponentService.setParameters('id', '@variant', { param1: 1, param2: 2 });
      expect(HstService.doPutForm).toHaveBeenCalledWith({ param1: 1, param2: 2 }, 'id', '%40variant');
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
    });

    it('URI-encodes the variant name', () => {
      HstComponentService.getProperties('id', '@variant');
      expect(HstService.doGet).toHaveBeenCalledWith('id', '%40variant', 'test-locale');
    });
  });

  describe('delete component', () => {
    it('uses the HstService to delete a component', () => {
      spyOn(HstService, 'doDelete').and.returnValue($q.resolve());
      HstComponentService.deleteComponent('containerId', 'componentId');
      expect(HstService.doDelete).toHaveBeenCalledWith('containerId', 'componentId');
    });
  });
});
