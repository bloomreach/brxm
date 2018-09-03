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

describe('ComponentEditorService', () => {
  let $q;
  let $rootScope;
  let ComponentEditor;
  let HstComponentService;

  const testData = {
    channel: 'channel',
    component: {
      id: 'componentId',
      label: 'componentLabel',
      variant: 'componentVariant',
    },
    container: 'container',
    page: 'page',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _ComponentEditor_, _HstComponentService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentEditor = _ComponentEditor_;
      HstComponentService = _HstComponentService_;
    });

    spyOn(HstComponentService, 'getProperties').and.returnValue($q.resolve({}));
  });

  describe('opening a component editor', () => {
    it('closes the previous editor', () => {
      spyOn(ComponentEditor, 'close');
      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(ComponentEditor.close).toHaveBeenCalled();
    });

    it('loads the component properties', () => {
      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(HstComponentService.getProperties).toHaveBeenCalledWith('componentId', 'componentVariant');
    });

    it('stores the editor data', () => {
      const properties = ['propertyData'];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(ComponentEditor.channel).toBe(testData.channel);
      expect(ComponentEditor.component).toBe(testData.component);
      expect(ComponentEditor.container).toBe(testData.container);
      expect(ComponentEditor.page).toBe(testData.page);
      expect(ComponentEditor.properties).toBe(properties);
    });
  });

  describe('getComponentName', () => {
    it('returns the component label if component is set', () => {
      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(ComponentEditor.getComponentName()).toBe('componentLabel');
    });

    it('returns undefined if component is not set', () => {
      expect(ComponentEditor.getComponentName()).toBeUndefined();
    });
  });
});
