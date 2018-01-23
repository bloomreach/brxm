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

describe('DocumentLocationField', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let CmsService;
  let FeedbackService;
  let Step1Service;
  let component;
  let getFolderSpy;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$componentController_,
      _$q_,
      _$rootScope_,
      _CmsService_,
      _FeedbackService_,
      _Step1Service_,
    ) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
      FeedbackService = _FeedbackService_;
      Step1Service = _Step1Service_;
    });

    component = $componentController('documentLocationField');
    getFolderSpy = spyOn(Step1Service, 'getFolders').and.returnValue($q.resolve());

    component.rootPath = '/root';
    component.changeLocale = () => angular.noop();
  });

  describe('$onInit', () => {
    it('throws an error if rootPath if not configured', () => {
      component.rootPath = undefined;
      expect(() => component.$onInit()).toThrowError('The rootPath option can not be empty');
    });

    it('throws an error if rootPath is a relative path', () => {
      component.rootPath = 'relative/path';
      expect(() => component.$onInit()).toThrowError('The rootPath option can only be an absolute path: relative/path');
    });

    it('throws an error if defaultPath is absolute', () => {
      component.defaultPath = '/path';
      expect(() => component.$onInit()).toThrowError('The defaultPath option can only be a relative path: /path');
    });

    it('detects the root path depth', () => {
      component.rootPath = '/root';
      component.$onInit();
      expect(component.rootPathDepth).toBe(1);

      component.rootPath = '/root/path';
      component.$onInit();
      expect(component.rootPathDepth).toBe(2);

      component.rootPath = '/root/path/';
      component.$onInit();
      expect(component.rootPathDepth).toBe(3);
    });

    it('stores the locale returned by the "getFolders" backend call', () => {
      const folders = [{ path: '/root' }, { path: '/root/path', locale: 'de' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.locale).toBe('de');
    });
  });

  describe('setting the document location', () => {
    it('stores the path of the last folder returned by the create-content-service', () => {
      const folders = [{ path: '/root' }, { path: '/root/path' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.documentLocation).toBe('/root/path');
    });

    it('stores the value of defaultPath returned by the create-content-service', () => {
      component.rootPath = '/root';
      const folders = [{ name: 'root' }, { name: 'default' }, { name: 'path' }];
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.$onInit();
      $rootScope.$apply();

      expect(component.defaultPath).toBe('default/path');
    });
  });

  describe('setting the document location label', () => {
    const setup = (rootPath, defaultPath, displayNames) => {
      const folders = [];
      displayNames.forEach((displayName) => {
        folders.push({ displayName, path: '' });
      });
      getFolderSpy.and.returnValue($q.resolve(folders));

      component.rootPath = rootPath;
      component.defaultPath = defaultPath;
      component.$onInit();
      $rootScope.$apply();
    };

    it('uses displayName(s) for the document location label', () => {
      setup('/root', '', ['R00T']);
      expect(component.documentLocationLabel).toBe('R00T');

      setup('/root', 'bloom', ['R00T', 'bl00m']);
      expect(component.documentLocationLabel).toBe('R00T/bl00m');
    });

    it('uses only one folder of root path if default path is empty', () => {
      setup('/channel/content', '', ['channel', 'content']);
      expect(component.documentLocationLabel).toBe('content');

      setup('/channel/content/root', '', ['channel', 'content', 'root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('/root', '', ['root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('/channel/content/root/path', '', ['channel', 'content', 'root', 'path']);
      expect(component.documentLocationLabel).toBe('path');

      setup('/root/path', '', ['root', 'path']);
      expect(component.documentLocationLabel).toBe('path');
    });

    it('uses only one folder of root path if default path depth is less than 3', () => {
      setup('/channel/content', 'some', ['channel', 'content', 'some']);
      expect(component.documentLocationLabel).toBe('content/some');

      setup('/channel/content', 'some/folder', ['channel', 'content', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('content/some/folder');

      setup('/channel/content/root', 'some/folder', ['channel', 'content', 'root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');

      setup('/root', 'some/folder', ['root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');
    });

    it('always shows a maximum of 3 folders', () => {
      setup('/channel/content/root', 'folder/with/document', ['channel', 'content', 'root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/document', ['root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/some/document', ['root', 'folder', 'with', 'some', 'document']);
      expect(component.documentLocationLabel).toBe('with/some/document');

      setup('/root', 'folder/with/some/nested/document', ['root', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');

      setup('/root/path', 'folder/with/some/nested/document', ['root', 'path', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');
    });
  });

  describe('onLoadFolders', () => {
    it('ignores an empty result from the backend', () => {
      component.documentLocationLabel = 'test-document-location-label';
      component.documentLocation = 'test-document-location';
      component.locale = 'test-locale';
      component.defaultPath = 'test-default-path';

      component.onLoadFolders([]);

      expect(component.documentLocationLabel).toBe('test-document-location-label');
      expect(component.documentLocation).toBe('test-document-location');
      expect(component.locale).toBe('test-locale');
      expect(component.defaultPath).toBe('test-default-path');
    });
  });

  describe('openPicker', () => {
    it('subscribes once to the "path-picked" event of the CMS before opening the picker', () => {
      spyOn(CmsService, 'subscribeOnce');
      component.openPicker();
      expect(CmsService.subscribeOnce).toHaveBeenCalledWith('path-picked', component.onPathPicked, component);
    });

    it('opens the picker by publishing the "show-path-picker" event', () => {
      spyOn(CmsService, 'publish');
      const pickerConfig = {};
      component.documentLocation = 'current-location';
      component.pickerConfig = pickerConfig;
      component.openPicker();
      expect(CmsService.publish).toHaveBeenCalledWith('show-path-picker', 'document-location-callback-id', 'current-location', pickerConfig);
    });
  });

  describe('onPathPicked', () => {
    it('only accepts callback events with callbackId "document-location-callback-id"', () => {
      spyOn(component, 'setDocumentLocation');

      component.onPathPicked('some-id', '/root/some-path');
      expect(component.setDocumentLocation).not.toHaveBeenCalled();

      component.onPathPicked('document-location-callback-id', '/root/new-path');
      expect(component.setDocumentLocation).toHaveBeenCalledWith('/root/new-path');
    });

    it('prepends relative paths with a /', () => {
      spyOn(component, 'setDocumentLocation');

      component.onPathPicked('document-location-callback-id', 'root/path');
      expect(component.setDocumentLocation).toHaveBeenCalledWith('/root/path');
    });

    it('shows an error if the chosen path is not part of the rootPath tree', () => {
      spyOn(FeedbackService, 'showError');
      spyOn(component, 'setDocumentLocation');

      component.onPathPicked('document-location-callback-id', '/flowers/tulip');
      expect(component.setDocumentLocation).not.toHaveBeenCalled();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DOCUMENT_LOCATION_NOT_ALLOWED', {
        root: '/root',
        path: '/flowers/tulip',
      });
    });
  });
});
