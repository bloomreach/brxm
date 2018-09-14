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
  let PageStructureService;

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

  function openComponentEditor(properties) {
    HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));
    ComponentEditor.open(testData);
    $rootScope.$digest();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$rootScope_, _ComponentEditor_, _HstComponentService_, _PageStructureService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentEditor = _ComponentEditor_;
      HstComponentService = _HstComponentService_;
      PageStructureService = _PageStructureService_;
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
      openComponentEditor(properties);

      expect(ComponentEditor.channel).toBe(testData.channel);
      expect(ComponentEditor.component).toBe(testData.component);
      expect(ComponentEditor.container).toBe(testData.container);
      expect(ComponentEditor.page).toBe(testData.page);
      expect(ComponentEditor.properties).toBe(properties);
    });
  });

  describe('getComponentName', () => {
    it('returns the component label if component is set', () => {
      openComponentEditor(['propertyData']);

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
      openComponentEditor([]);

      expect(ComponentEditor.getPropertyGroups().length).toBe(0);
    });

    it('does not include a property that must be hidden', () => {
      const properties = [
        { name: 'hidden', hiddenInChannelManager: true },
        { name: 'visible', hiddenInChannelManager: false, groupLabel: 'Group' },
      ];
      openComponentEditor(properties);

      const groups = ComponentEditor.getPropertyGroups();
      expectGroup(groups[0], 'Group', 1);
      expect(groups[0].fields[0].name).toBe('visible');
    });

    it('does not create a group if it only has hidden properties', () => {
      openComponentEditor([
        { groupLabel: 'Group', hiddenInChannelManager: true },
        { groupLabel: 'Group', hiddenInChannelManager: true },
        { groupLabel: 'Group1', hiddenInChannelManager: false },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(1);
      expectGroup(groups[0], 'Group1', 1);
    });

    it('uses the default group label for properties with an empty string label', () => {
      openComponentEditor([
        { groupLabel: '' },
        { groupLabel: null },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expectGroup(groups[0], 'DEFAULT_PROPERTY_GROUP_LABEL', 1);
    });

    it('puts all the fields with the same label in one group', () => {
      openComponentEditor([
        { groupLabel: '' },
        { groupLabel: 'Group' },
        { groupLabel: null },
        { groupLabel: '' },
        { groupLabel: 'Group' },
        { groupLabel: null },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(3);
      expectGroup(groups[0], 'DEFAULT_PROPERTY_GROUP_LABEL', 2);
      expectGroup(groups[1], 'Group', 2);
      expectGroup(groups[2], null, 2, false);
    });

    it('adds the template chooser property into a non-collapsible group with label "org.hippoecm.hst.core.component.template"', () => {
      openComponentEditor([
        { name: 'org.hippoecm.hst.core.component.template', groupLabel: 'Group' },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(1);
      expectGroup(groups[0], 'org.hippoecm.hst.core.component.template', 1, false);
    });

    it('adds properties with a null group label into a non-collapsible group', () => {
      openComponentEditor([
        { groupLabel: null },
        { groupLabel: 'Group' },
        { groupLabel: 'Group2' },
        { groupLabel: null },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups.length).toBe(3);
      expectGroup(groups[0], null, 2, false);
    });

    it('uses the property\'s defaultValue if it is defined/not-null/not-empty and the property value is null', () => {
      openComponentEditor([
        { value: null },
        { value: null, defaultValue: null },
        { value: null, defaultValue: '' },
        { value: null, defaultValue: 'defaultValue' },
        { value: '', defaultValue: 'defaultValue' },
        { value: false, defaultValue: true },
      ]);

      const fields = ComponentEditor.getPropertyGroups()[0].fields;
      expect(fields[0].value).toBe(null);
      expect(fields[1].value).toBe(null);
      expect(fields[2].value).toBe(null);
      expect(fields[3].value).toBe('defaultValue');
      expect(fields[4].value).toBe('');
      expect(fields[5].value).toBe(false);
    });
  });

  describe('normalizing properties', () => {
    const linkPickerProperty = {
      type: 'linkpicker',
      pickerConfiguration: 'pickerConfiguration',
      pickerRemembersLastVisited: 'pickerRemembersLastVisited',
      pickerInitialPath: 'pickerInitialPath',
      pickerPathIsRelative: 'pickerPathIsRelative',
      pickerRootPath: 'pickerRootPath',
      pickerSelectableNodeTypes: 'pickerSelectableNodeTypes',
    };

    it('extracts the link picker config if property type is "linkpicker"', () => {
      openComponentEditor([
        linkPickerProperty,
        Object.assign({}, linkPickerProperty, { type: 'imagepicker' }),
      ]);

      const group = ComponentEditor.getPropertyGroups()[0];
      const linkPickerField = group.fields[0];

      expect(linkPickerField.pickerConfig).toBeDefined();
      expect(linkPickerField.pickerConfig.linkpicker).toBeDefined();
      expect(linkPickerField.pickerConfig.linkpicker.configuration).toBe('pickerConfiguration');
      expect(linkPickerField.pickerConfig.linkpicker.remembersLastVisited).toBe('pickerRemembersLastVisited');
      expect(linkPickerField.pickerConfig.linkpicker.initialPath).toBe('pickerInitialPath');
      expect(linkPickerField.pickerConfig.linkpicker.isRelativePath).toBe('pickerPathIsRelative');
      expect(linkPickerField.pickerConfig.linkpicker.rootPath).toBe('pickerRootPath');
      expect(linkPickerField.pickerConfig.linkpicker.selectableNodeTypes).toBe('pickerSelectableNodeTypes');

      const imagePickerField = group.fields[1];

      expect(imagePickerField.pickerConfig).not.toBeDefined();
    });

    it('marks the property as a "path-picker" if property type is "linkpicker"', () => {
      openComponentEditor([
        linkPickerProperty,
      ]);

      const group = ComponentEditor.getPropertyGroups()[0];
      const linkPickerField = group.fields[0];
      expect(linkPickerField.pickerConfig.linkpicker.isPathPicker).toBe(true);
    });
  });

  describe('valueChanged', () => {
    it('transforms the "properties" data and passes it to the PageStructureService to render the component', () => {
      spyOn(PageStructureService, 'renderComponent');
      const properties = [
        { name: 'a', value: 'value-a' },
        { name: 'b', value: 'value-b' },
        { name: 'c', value: 'value-c', hiddenInChannelManager: true },
      ];
      openComponentEditor(properties);

      ComponentEditor.valueChanged();

      expect(PageStructureService.renderComponent).toHaveBeenCalledWith('componentId', {
        a: 'value-a',
        b: 'value-b',
        c: 'value-c',
      });
    });
  });
});
