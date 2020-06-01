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
  let ChannelService;
  let CmsService;
  let FeedbackService;
  let PickerService;
  let Step1Service;
  let component;
  let getFolderSpy;
  let inputOverlaySpy;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    ChannelService = jasmine.createSpyObj('ChannelService', ['initialize', 'getChannel']);
    ChannelService.getChannel.and.returnValue({
      contentRoot: '/channel/content',
    });

    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('ChannelService', ChannelService);
      $provide.value('PickerService', PickerService);
    });

    inject((
      _$componentController_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _CmsService_,
      _FeedbackService_,
      _Step1Service_,
    ) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      FeedbackService = _FeedbackService_;
      Step1Service = _Step1Service_;
    });

    const $element = jasmine.createSpyObj('$element', ['find']);

    inputOverlaySpy = jasmine.createSpyObj('input-overlay', ['focus']);
    $element.find.and.returnValue(inputOverlaySpy);

    component = $componentController('documentLocationField', {
      $element,
    });
    getFolderSpy = spyOn(Step1Service, 'getFolders').and.returnValue($q.resolve());
    spyOn(CmsService, 'reportUsageStatistic');

    component.changeLocale = () => angular.noop();
  });

  describe('with root path', () => {
    beforeEach(() => {
      component.rootPath = '/root';
    });

    describe('$onInit', () => {
      it('sets the initialPickerPath to the root path', () => {
        component.$onInit();
        expect(component.initialPickerPath).toBe('/root');
      });

      it('throws an error if rootPath is a relative path', () => {
        component.rootPath = 'relative/path';
        expect(() => component.$onInit())
          .toThrowError('The rootPath option can only be an absolute path: relative/path');
      });

      it('throws an error if defaultPath is absolute', () => {
        component.defaultPath = '/path';
        expect(() => component.$onInit()).toThrowError('The defaultPath option can only be a relative path: /path');
      });

      it('sets the default picker config', () => {
        component.$onInit();
        expect(component.pickerPath).toBe('/');
        expect(component.pickerConfig).toEqual({
          configuration: 'cms-pickers/documents-folders-only',
          rootPath: '/root',
          selectableNodeTypes: ['hippostd:folder'],
        });
      });

      it('sets a path according to rootPath and defaultPath configuration', () => {
        spyOn(component, 'setPath');
        component.$onInit();
        expect(component.setPath).toHaveBeenCalledWith('/root');

        component.defaultPath = 'desk/paper';
        component.$onInit();
        expect(component.setPath).toHaveBeenCalledWith('/root/desk/paper');
      });
    });

    describe('setting the document location', () => {
      it('stores the path and defaultPath of the last folder returned by the create-content-service', () => {
        const folders = [{ path: '/root' }, { path: '/root/content' }, { path: '/root/content/document' }];
        getFolderSpy.and.returnValue($q.resolve(folders));

        component.$onInit();
        $rootScope.$apply();

        expect(component.path).toBe('/root/content/document');
        expect(component.defaultPath).toBe('content/document');
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
        expect(component.pathLabel).toBe('/R00T');

        setup('/root/water', 'bloom/flower', ['R00T', 'water', 'bloom', 'fl0w3r']);
        expect(component.pathLabel).toBe('/R00T/water/bloom/fl0w3r');
      });
    });

    describe('onLoadFolders', () => {
      it('ignores an empty result from the backend', () => {
        component.pathLabel = 'test-document-location-label';
        component.path = 'test-document-location';
        component.locale = 'test-locale';
        component.defaultPath = 'test-default-path';
        component.pickerPath = 'test-picker-path';

        component.onLoadFolders([]);

        expect(component.pathLabel).toBe('test-document-location-label');
        expect(component.path).toBe('test-document-location');
        expect(component.locale).toBe('test-locale');
        expect(component.defaultPath).toBe('test-default-path');
        expect(component.pickerPath).toBe('test-picker-path');
      });

      it('stores the pickerPath, i.e. the deepest nested folder that exists in the repository', () => {
        component.onLoadFolders([
          {
            displayName: 'A', name: 'a', path: '/a', exists: true,
          },
          {
            displayName: 'B', name: 'b', path: '/a/b', exists: true,
          },
          {
            displayName: 'C', name: 'c', path: '/a/b/c', exists: false,
          },
        ]);
        expect(component.pickerPath).toBe('/a/b');
      });

      it('stores / as the pickerPath if none of the folders already exist in the repository', () => {
        component.onLoadFolders([
          {
            displayName: 'A', name: 'a', path: '/a', exists: false,
          },
          {
            displayName: 'B', name: 'b', path: '/a/b', exists: false,
          },
        ]);
        expect(component.pickerPath).toBe('/');
      });

      it('stores the locale of the last folder', () => {
        const folders = [{ path: '/root', locale: 'fr' }, { path: '/root/path', locale: 'de' }];
        getFolderSpy.and.returnValue($q.resolve(folders));

        component.$onInit();
        $rootScope.$apply();

        expect(component.locale).toBe('de');
      });
    });

    describe('openPicker', () => {
      it('opens the picker', () => {
        PickerService.pickPath.and.returnValue($q.resolve());
        const pickerConfig = {};
        component.pickerPath = 'current-location';
        component.pickerConfig = pickerConfig;
        component.openPicker();
        expect(PickerService.pickPath).toHaveBeenCalledWith(pickerConfig, 'current-location');
      });
    });

    describe('onPathPicked', () => {
      beforeEach(() => {
        component.initialPickerPath = component.rootPath;
      });

      it('uses absolute paths as-is', () => {
        spyOn(component, 'setPath');

        component.onPathPicked('/root/path');
        expect(component.setPath).toHaveBeenCalledWith('/root/path');
      });

      it('prepends relative paths with a /', () => {
        spyOn(component, 'setPath');

        component.onPathPicked('root/path');
        expect(component.setPath).toHaveBeenCalledWith('/root/path');
      });

      it('shows an error if the chosen path is not part of the rootPath tree', () => {
        spyOn(FeedbackService, 'showError');
        spyOn(component, 'setPath');

        component.onPathPicked('/flowers/tulip');
        expect(component.setPath).not.toHaveBeenCalled();
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DOCUMENT_LOCATION_NOT_ALLOWED', {
          root: '/root',
          path: '/flowers/tulip',
        });
      });
    });
  });

  describe('without root path', () => {
    describe('and without default path', () => {
      describe('$onInit', () => {
        it('sets the initialPickerPath to the channel content root', () => {
          component.$onInit();
          expect(component.initialPickerPath).toBe('/channel/content');
        });

        it('does not set the rootPath', () => {
          component.$onInit();
          expect(component.rootPath).toBeUndefined();
        });

        it('sets the default picker config', () => {
          component.$onInit();
          expect(component.pickerPath).toBe('/');
          expect(component.pickerConfig).toEqual({
            configuration: 'cms-pickers/documents-folders-only',
            rootPath: '/channel/content',
            selectableNodeTypes: ['hippostd:folder'],
          });
        });
      });
    });

    describe('but with default path', () => {
      describe('$onInit', () => {
        beforeEach(() => {
          component.defaultPath = 'default/path';
        });

        it('sets the initialPickerPath to the channel content root', () => {
          component.$onInit();
          expect(component.initialPickerPath).toBe('/channel/content');
        });

        it('does set the rootPath', () => {
          component.$onInit();
          expect(component.rootPath).toBe('/channel/content');
        });

        it('sets the default picker config with root path of channel path plus default path', () => {
          component.$onInit();
          expect(component.pickerPath).toBe('/');
          expect(component.pickerConfig).toEqual({
            configuration: 'cms-pickers/documents-folders-only',
            rootPath: '/channel/content',
            selectableNodeTypes: ['hippostd:folder'],
          });
        });
      });
    });


    describe('onPathPicked', () => {
      beforeEach(() => {
        component.initialPickerPath = '/channel/content';
      });

      it('uses absolute paths as-is', () => {
        spyOn(component, 'setPath');

        component.onPathPicked('/channel/content/new-path');
        expect(component.setPath).toHaveBeenCalledWith('/channel/content/new-path');
      });

      it('prepends relative paths with a /', () => {
        spyOn(component, 'setPath');

        component.onPathPicked('channel/content/path');
        expect(component.setPath).toHaveBeenCalledWith('/channel/content/path');
      });

      it('shows an error if the chosen path is not part of the rootPath tree', () => {
        spyOn(FeedbackService, 'showError');
        spyOn(component, 'setPath');

        component.onPathPicked('/flowers/tulip');
        expect(component.setPath).not.toHaveBeenCalled();
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DOCUMENT_LOCATION_NOT_ALLOWED', {
          root: '/channel/content',
          path: '/flowers/tulip',
        });
      });

      it('it is allowed to subsequently choose a shorter path', () => {
        // the path check is done on the original picker path, not on the last selected rootpath
        spyOn(component, 'setPath');
        component.onPathPicked('/channel/content/folder1/folder2');
        component.onPathPicked('/channel/content/folder1');
        expect(component.setPath).toHaveBeenCalled();
      });
    });
  });

  describe('onPathCancelled', () => {
    it('focuses the input overlay', () => {
      spyOn(component, 'setPath');
      component.onPathCanceled();
      expect(inputOverlaySpy.focus).toHaveBeenCalled();
    });
  });
});
