/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

fdescribe('DocumentLocationField', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let ChannelService;
  let CreateContentService;
  let FeedbackService;

  let component;
  let $scope;
  let getFolderSpy;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((_$componentController_, _$q_, _$rootScope_, _ChannelService_, _CreateContentService_, _FeedbackService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
    });

    $scope = $rootScope.$new();
    component = $componentController('documentLocationField');

    getFolderSpy = spyOn(CreateContentService, 'getFolders').and.returnValue($q.resolve());
  });


  describe('parsing the rootPath @Input', () => {
    it('defaults to the channel root if not set', () => {
      // $rootScope.$apply();
      expect(component.rootPath).toBe('/channel/content');
    });

    it('overrides the channel root path if absolute', () => {
      component.rootPath = '/root/path';
      $rootScope.$apply();
      expect(component.rootPath).toBe('/root/path');
    });

    it('is concatenated wth the channel\'s root path if relative', () => {
      component.rootPath = 'some/path';
      $rootScope.$apply();
      expect(component.rootPath).toBe('/channel/content/some/path');
    });

    it('never ends with a slash', () => {
      component.rootPath = '/root/path/';
      $rootScope.$apply();
      expect(component.rootPath).toBe('/root/path');

      component.rootPath = 'some/path/';
      component.ngOnInit();
      expect(component.rootPath).toBe('/channel/content/some/path');
    });

    it('detects the root path depth', () => {
      component.rootPath = '/root';
      $rootScope.$apply();
      expect(component.rootPathDepth).toBe(1);

      component.rootPath = '/root/path/';
      component.ngOnInit();
      expect(component.rootPathDepth).toBe(2);

      component.rootPath = 'some/path/';
      component.ngOnInit();
      expect(component.rootPathDepth).toBe(4);
    });
  });

  describe('parsing the defaultPath @Input', () => {
    it('throws an error if defaultPath is absolute', () => {
      component.defaultPath = '/path';
      expect(() => $rootScope.$apply()).toThrow(new Error('The defaultPath option can only be a relative path'));
    });
  });

  describe('setting the document location', () => {
    it('stores the path of the last folder returned by the create-content-service', () => {
      const folders = [{path: '/root'}, {path: '/root/path'}];
      getFolderSpy.and.returnValue($q.resolve(folders));
      component.$onInit();
      expect(component.documentLocation).toBe('/root/path');
    });

    it('stores the value of defaultPath returned by the create-content-service', () => {
      component.rootPath = '/root';
      const folders = [{name: 'root'}, {name: 'default'}, {name: 'path'}];
      getFolderSpy.and.returnValue($q.resolve(folders));
      component.$onInit();
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
      setup('', '', ['channel', 'content']);
      expect(component.documentLocationLabel).toBe('content');

      setup('root', '', ['channel', 'content', 'root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('/root', '', ['root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('root/path', '', ['channel', 'content', 'root', 'path']);
      expect(component.documentLocationLabel).toBe('path');

      setup('/root/path', '', ['root', 'path']);
      expect(component.documentLocationLabel).toBe('path');
    });

    it('uses only one folder of root path if default path depth is less than 3', () => {
      setup('', 'some', ['channel', 'content', 'some']);
      expect(component.documentLocationLabel).toBe('content/some');

      setup('', 'some/folder', ['channel', 'content', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('content/some/folder');

      setup('root', 'some/folder', ['channel', 'content', 'root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');

      setup('/root', 'some/folder', ['root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');
    });

    it('always shows a maximum of 3 folders', () => {
      setup('root', 'folder/with/document', ['channel', 'content', 'root', 'folder', 'with', 'document']);
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

  describe('the locale @Output', () => {
    it('emits the locale when component is initialized', () => {
      let changedLocale;
      component.changeLocale.subscribe((locale) => changedLocale = locale);

      const folders = [{path: '/root'}, {path: '/root/path', locale: 'de'}];
      getFolderSpy.and.returnValue($q.resolve(folders));
      component.$onInit();
      expect(changedLocale).toBe('de');
    });
  });
});
