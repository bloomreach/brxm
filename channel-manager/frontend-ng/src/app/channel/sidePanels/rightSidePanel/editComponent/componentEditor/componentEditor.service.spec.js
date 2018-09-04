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
      const properties = ['propertyData'];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(ComponentEditor.getComponentName()).toBe('componentLabel');
    });

    it('returns undefined if component is not set', () => {
      expect(ComponentEditor.getComponentName()).toBeUndefined();
    });
  });

  describe('ordering properties into groups', () => {
    function expectGroup(group, label, length, collapse = true) {
      expect(group.label).toBe(label);
      expect(group.fields.length).toBe(length);
      expect(group.collapse).toBe(collapse);
    }

    it('does not create a group when there are no properties', () => {
      const properties = [];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(ComponentEditor.getPropertyGroups().length).toBe(0);
    });

    it('does not include a property that must be hidden', () => {
      const properties = [
        { name: 'hidden', hiddenInChannelManager: true },
        { name: 'visible', hiddenInChannelManager: false, groupLabel: 'Group' },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expectGroup(groups[0], 'Group', 1);
      expect(groups[0].fields[0].name).toBe('visible');
    });

    it('does not create a group if it only has hidden properties', () => {
      const properties = [
        { groupLabel: 'Group', hiddenInChannelManager: true },
        { groupLabel: 'Group', hiddenInChannelManager: true },
        { groupLabel: 'Group1', hiddenInChannelManager: false },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(1);
      expectGroup(groups[0], 'Group1', 1);
    });

    it('uses the default group label for properties with an empty string label', () => {
      const properties = [
        { groupLabel: '' },
        { groupLabel: null },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expectGroup(groups[0], 'DEFAULT_PROPERTY_GROUP_LABEL', 1);
    });

    it('puts all the fields with the same label in one group', () => {
      const properties = [
        { groupLabel: '' },
        { groupLabel: 'Group' },
        { groupLabel: null },
        { groupLabel: '' },
        { groupLabel: 'Group' },
        { groupLabel: null },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(3);
      expectGroup(groups[0], 'DEFAULT_PROPERTY_GROUP_LABEL', 2);
      expectGroup(groups[1], 'Group', 2);
      expectGroup(groups[2], null, 2, false);
    });

    it('adds the template chooser property into a non-collapsible group with label "org.hippoecm.hst.core.component.template"', () => {
      const properties = [
        { name: 'org.hippoecm.hst.core.component.template', groupLabel: 'Group' },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(1);
      expectGroup(groups[0], 'org.hippoecm.hst.core.component.template', 1, false);
    });

    it('adds properties with a null group label into a non-collapsible group', () => {
      const properties = [
        { groupLabel: null },
        { groupLabel: 'Group' },
        { groupLabel: 'Group2' },
        { groupLabel: null },
      ];
      HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));

      ComponentEditor.open(testData);
      $rootScope.$digest();

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(3);
      expectGroup(groups[0], null, 2, false);
    });
  });
});
