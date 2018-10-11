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
  let $translate;
  let ComponentEditor;
  let DialogService;
  let FeedbackService;
  let HippoIframeService;
  let HstComponentService;
  let PageStructureService;

  let testData;
  const emptyValues = [undefined, null, ''];
  const testValues = emptyValues.concat('test');

  function openComponentEditor(properties) {
    HstComponentService.getProperties.and.returnValue($q.resolve({ properties }));
    ComponentEditor.open(testData);
    $rootScope.$digest();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((_PageStructureService_) => {
      spyOn(_PageStructureService_, 'registerChangeListener');
    });

    inject((
      _$q_,
      _$rootScope_,
      _$translate_,
      _ComponentEditor_,
      _DialogService_,
      _FeedbackService_,
      _HippoIframeService_,
      _HstComponentService_,
      _PageStructureService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      ComponentEditor = _ComponentEditor_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      HstComponentService = _HstComponentService_;
      PageStructureService = _PageStructureService_;
    });

    spyOn(HstComponentService, 'getProperties').and.returnValue($q.resolve({}));
    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.resolve({}));

    testData = {
      channel: 'channel',
      component: {
        id: 'componentId',
        label: 'componentLabel',
        variant: 'componentVariant',
      },
      container: {
        id: 'containerId',
        isDisabled: false,
      },
      page: 'page',
    };
  });

  describe('responding to page structure changes', () => {
    let onStructureChange;

    beforeEach(() => {
      spyOn(ComponentEditor, 'reopen');
      spyOn(PageStructureService, 'getComponentById');
      onStructureChange = PageStructureService.registerChangeListener.calls.mostRecent().args[0];

      ComponentEditor.component = { id: 'some-id' };
      ComponentEditor.container = {
        isDisabled: false,
        isInherited: false,
        id: 1,
      };
    });

    it('should do nothing without a component', () => {
      delete ComponentEditor.component;
      onStructureChange();
      expect(PageStructureService.getComponentById).not.toHaveBeenCalled();
    });

    it('should not update when the component is not on the page', () => {
      PageStructureService.getComponentById.and.returnValue(null);
      onStructureChange();

      expect(PageStructureService.getComponentById).toHaveBeenCalledWith('some-id');
      expect(ComponentEditor.container.id).toBe(1);
    });

    it('should update the container information if it has changed', () => {
      PageStructureService.getComponentById.and.returnValue({
        container: {
          isDisabled: () => false,
          isInherited: () => true,
          getId: () => 2,
        },
      });
      onStructureChange();

      expect(ComponentEditor.container.isDisabled).toBe(false);
      expect(ComponentEditor.container.isInherited).toBe(true);
      expect(ComponentEditor.container.id).toBe(2);
      expect(ComponentEditor.reopen).not.toHaveBeenCalled();
    });

    it('should reopen editor', () => {
      PageStructureService.getComponentById.and.returnValue({
        container: {
          isDisabled: () => true,
          isInherited: () => true,
          getId: () => 2,
        },
      });
      onStructureChange();

      expect(ComponentEditor.reopen).toHaveBeenCalled();
    });
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
      const properties = [{ name: 'test-property' }];
      openComponentEditor(properties);

      expect(ComponentEditor.channel).toBe(testData.channel);
      expect(ComponentEditor.component).toBe(testData.component);
      expect(ComponentEditor.container).toBe(testData.container);
      expect(ComponentEditor.page).toBe(testData.page);
      expect(ComponentEditor.properties).toBe(properties);
    });

    it('reloads the page and shows a message when retrieving properties returns an error', () => {
      spyOn(FeedbackService, 'showError');
      spyOn(HippoIframeService, 'reload');

      HstComponentService.getProperties.and.returnValue($q.reject());

      ComponentEditor.open(testData);
      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT');
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });

  describe('reopening the editor', () => {
    it('opens the editor for the component is was opened for originally', () => {
      openComponentEditor();

      spyOn(ComponentEditor, 'open').and.returnValue($q.resolve());
      ComponentEditor.reopen();
      expect(ComponentEditor.open).toHaveBeenCalledWith(testData);
    });
  });

  describe('getComponentId', () => {
    it('returns the component identifier if component is set', () => {
      openComponentEditor(['propertyData']);

      expect(ComponentEditor.getComponentId()).toBe('componentId');
    });

    it('returns undefined if component is not set', () => {
      expect(ComponentEditor.getComponentId()).toBeUndefined();
    });
  });

  describe('getComponentName', () => {
    it('returns the component label if component is set', () => {
      openComponentEditor();

      expect(ComponentEditor.getComponentName()).toBe('componentLabel');
    });

    it('returns the display name of the error when no component is set and there is an error', () => {
      ComponentEditor.error = {
        messageParams: {
          displayName: 'componentLabel',
        },
      };
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

    it('marks the group with the default group label', () => {
      openComponentEditor([
        { groupLabel: '' },
        { groupLabel: 'test' },
      ]);

      const groups = ComponentEditor.getPropertyGroups();
      expect(groups[0].default).toBe(true);
      expect(groups[1].default).toBe(false);
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
  });

  describe('setDefaultIfValueIsEmpty', () => {
    it('does not error if property does not exist', () => {
      try {
        ComponentEditor.setDefaultIfValueIsEmpty();
        ComponentEditor.setDefaultIfValueIsEmpty(null);
      } catch (e) {
        fail();
      }
    });

    it('sets the property value if it is empty and the default value is not empty', () => {
      emptyValues.forEach((emptyValue) => {
        const property = { value: emptyValue, defaultValue: 'defaultValue' };
        ComponentEditor.setDefaultIfValueIsEmpty(property);

        expect(property.value).toBe('defaultValue');
      });
    });

    it('does not change the property value if the default value is empty', () => {
      testValues.forEach((value) => {
        emptyValues.forEach((defaultValue) => {
          const property = { value, defaultValue };
          ComponentEditor.setDefaultIfValueIsEmpty(property);

          expect(property.value).toBe(value);
        });
      });
    });

    it('sets the display value for "linkpicker" fields if it is empty and default display value is not empty', () => {
      emptyValues.forEach((emptyValue) => {
        const property = { displayValue: emptyValue, defaultDisplayValue: 'default-display-value', type: 'linkpicker' };
        ComponentEditor.setDefaultIfValueIsEmpty(property);

        expect(property.displayValue).toBe('default-display-value');
      });
    });

    it('does not change the display value for "linkpicker" fields if the default display value is empty', () => {
      testValues.forEach((value) => {
        emptyValues.forEach((emptyValue) => {
          const property = {
            displayValue: value,
            defaultDisplayValue: emptyValue,
            type: 'linkpicker',
          };
          ComponentEditor.setDefaultIfValueIsEmpty(property);

          expect(property.displayValue).toBe(value);
        });
      });
    });
  });

  describe('handling of the default value when opening the component editor', () => {
    it('tries to set the default value for each non-hidden property', () => {
      spyOn(ComponentEditor, 'setDefaultIfValueIsEmpty');

      openComponentEditor();
      expect(ComponentEditor.setDefaultIfValueIsEmpty).not.toHaveBeenCalled();

      openComponentEditor([{ hiddenInChannelManager: true }]);
      expect(ComponentEditor.setDefaultIfValueIsEmpty).not.toHaveBeenCalled();

      openComponentEditor([{}, {}]);
      expect(ComponentEditor.setDefaultIfValueIsEmpty).toHaveBeenCalledTimes(2);
    });

    function loadProperty(property) {
      openComponentEditor([property]);

      const fields = ComponentEditor.getPropertyGroups()[0].fields;
      const field = fields[0];
      return {
        andExpect: fieldName => expect(field[fieldName]),
      };
    }

    describe('checkbox fields', () => {
      const onValues = [true, 'true', 1, '1', 'on', 'On', 'ON'];
      const offValues = [false, 'false', 0, '0', 'off', 'Off', 'OFF'];
      const type = 'checkbox';

      it('defaults to "off" for checkbox fields', () => {
        testValues.forEach(defaultValue => loadProperty({ defaultValue, type }).andExpect('value').toBe('off'));
      });

      it('normalizes value to "on" and "off"', () => {
        onValues.forEach(value => loadProperty({ value, type }).andExpect('value').toBe('on'));
        offValues.forEach(value => loadProperty({ value, type }).andExpect('value').toBe('off'));
      });

      it('normalizes defaultValue to "on" and "off"', () => {
        onValues.forEach(defaultValue => loadProperty({ defaultValue, type }).andExpect('defaultValue').toBe('on'));
        offValues.forEach(defaultValue => loadProperty({ defaultValue, type }).andExpect('defaultValue').toBe('off'));
      });
    });

    describe('linkpicker fields', () => {
      const type = 'linkpicker';

      it('sets the default display value for "linkpicker" fields to the last segment of a path if the default value is set and differs from the value', () => {
        loadProperty({ defaultValue: 'a', type }).andExpect('defaultDisplayValue').toBe('a');
        loadProperty({ defaultValue: '/a/b/c', type }).andExpect('defaultDisplayValue').toBe('c');
      });

      it('does not change the default display value if default value is empty', () => {
        emptyValues.forEach(defaultValue => loadProperty({ defaultValue, defaultDisplayValue: 'default-display-value', type })
          .andExpect('defaultDisplayValue').toBe('default-display-value'));
      });
    });
  });

  describe('read-only state', () => {
    it('is undefined when the container is unknown', () => {
      expect(ComponentEditor.isReadOnly()).toBeUndefined();
    });

    it('is false when the container is not disabled', () => {
      openComponentEditor([]);
      expect(ComponentEditor.isReadOnly()).toBe(false);
    });

    it('is true when the container is disabled', () => {
      testData.container.isDisabled = true;
      openComponentEditor([]);
      expect(ComponentEditor.isReadOnly()).toBe(true);
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

  describe('updatePreview', () => {
    it('transforms the "properties" data and passes it to the PageStructureService to render the component', (done) => {
      spyOn(PageStructureService, 'renderComponent').and.returnValue($q.resolve());
      const properties = [
        { name: 'a', value: 'value-a' },
        { name: 'b', value: '' },
        { name: 'c', value: '', defaultValue: 'value-c' },
        { name: 'd', value: 'value-d', hiddenInChannelManager: true },
      ];
      openComponentEditor(properties);
      properties[1].value = 'value-b';

      ComponentEditor.updatePreview().then(() => {
        expect(PageStructureService.renderComponent).toHaveBeenCalledWith('componentId', {
          a: 'value-a',
          b: 'value-b',
          c: 'value-c',
          d: 'value-d',
        });
        done();
      });
      $rootScope.$digest();
    });

    it('only passes the first 10 characters of a date field value', (done) => {
      spyOn(PageStructureService, 'renderComponent').and.returnValue($q.resolve());
      const properties = [
        { name: 'a', value: '2017-09-21T00:00:00.000+02:00', type: 'datefield' },
      ];
      openComponentEditor(properties);

      ComponentEditor.updatePreview().then(() => {
        expect(PageStructureService.renderComponent).toHaveBeenCalledWith('componentId', {
          a: '2017-09-21',
        });
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('save', () => {
    it('stores the new parameter values as form data', () => {
      spyOn(HstComponentService, 'setParameters').and.returnValue($q.resolve());

      const properties = [
        { name: 'a', value: 'value-a' },
        { name: 'b', value: '' },
        { name: 'c', value: '', defaultValue: 'value-c' },
      ];
      openComponentEditor(properties);
      properties[1].value = 'value-b';

      ComponentEditor.save();

      expect(HstComponentService.setParameters).toHaveBeenCalledWith('componentId', 'componentVariant', {
        a: 'value-a',
        b: 'value-b',
        c: 'value-c',
      });
    });
  });

  describe('delete component functions', () => {
    beforeEach(() => {
      openComponentEditor();
    });

    it('calls the hst component service for deleteComponent', () => {
      ComponentEditor.deleteComponent();
      expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('containerId', 'componentId');
    });

    it('calls the dialog service for delete component confirmation', () => {
      const showPromise = {};
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(DialogService, 'show');

      DialogService.show.and.returnValue(showPromise);

      const result = ComponentEditor.confirmDeleteComponent();

      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('clears the state when the component is successfully deleted', () => {
      spyOn(ComponentEditor, 'close');
      HstComponentService.deleteComponent.and.returnValue($q.reject());

      ComponentEditor.deleteComponent();
      $rootScope.$digest();

      expect(ComponentEditor.close).not.toHaveBeenCalled();

      HstComponentService.deleteComponent.and.returnValue($q.resolve());

      ComponentEditor.deleteComponent();
      $rootScope.$digest();

      expect(ComponentEditor.close).toHaveBeenCalled();
    });
  });

  describe('the close function', () => {
    it('clears all properties so a next call starts with a clean slate', () => {
      ComponentEditor.channel = {};
      ComponentEditor.component = {};
      ComponentEditor.container = {};
      ComponentEditor.killed = false;
      ComponentEditor.page = {};
      ComponentEditor.properties = {};
      ComponentEditor.propertyGroups = {};
      ComponentEditor.error = 'error';

      ComponentEditor.close();

      expect(ComponentEditor.channel).toBeUndefined();
      expect(ComponentEditor.component).toBeUndefined();
      expect(ComponentEditor.container).toBeUndefined();
      expect(ComponentEditor.killed).toBeUndefined();
      expect(ComponentEditor.page).toBeUndefined();
      expect(ComponentEditor.properties).toBeUndefined();
      expect(ComponentEditor.propertyGroups).toBeUndefined();
      expect(ComponentEditor.error).toBeUndefined();
    });
  });

  describe('discard changes functions', () => {
    beforeEach(() => {
      openComponentEditor();
    });

    it('calls the dialog service to confirm', () => {
      const showPromise = {};
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(DialogService, 'show');

      DialogService.show.and.returnValue(showPromise);

      const result = ComponentEditor.confirmDiscardChanges();

      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('reopens the component editor to discard changes', () => {
      spyOn(ComponentEditor, 'reopen').and.returnValue($q.resolve());
      ComponentEditor.discardChanges();
      expect(ComponentEditor.reopen).toHaveBeenCalled();
    });

    it('reloads the page when discarding changes succeeded', () => {
      spyOn(HippoIframeService, 'reload').and.returnValue($q.resolve());
      spyOn(ComponentEditor, 'reopen').and.returnValue($q.resolve());

      ComponentEditor.discardChanges();
      $rootScope.$digest();

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('reloads the page when discarding changes failed', () => {
      spyOn(HippoIframeService, 'reload').and.returnValue($q.resolve());
      spyOn(ComponentEditor, 'open').and.returnValue($q.reject());

      ComponentEditor.discardChanges();
      $rootScope.$digest();

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });

  describe('confirm save or discard changes', () => {
    beforeEach(() => {
      ComponentEditor.component = {
        id: 'component-id',
        label: 'component-label',
      };

      spyOn($translate, 'instant');
      spyOn(DialogService, 'alert');
      spyOn(DialogService, 'show').and.returnValue($q.resolve());
    });

    it('shows a dialog', (done) => {
      ComponentEditor.confirmSaveOrDiscardChanges(true)
        .then(() => {
          expect(DialogService.show).toHaveBeenCalled();
          done();
        });
      $rootScope.$digest();
    });

    it('translates the dialog title and body text', (done) => {
      ComponentEditor.confirmSaveOrDiscardChanges(true)
        .then(() => {
          expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
          expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TO_COMPONENT', {
            componentLabel: 'component-label',
          });
          done();
        });
      $rootScope.$digest();
    });

    describe('with valid data', () => {
      it('resolves with "SAVE" when the dialog resolves with "SAVE", and does not show an alert nor redraw the component', (done) => {
        spyOn(ComponentEditor, 'save').and.returnValue($q.resolve());
        spyOn(PageStructureService, 'renderComponent');
        DialogService.show.and.returnValue($q.resolve('SAVE'));

        ComponentEditor.confirmSaveOrDiscardChanges(true)
          .then((action) => {
            expect(action).toBe('SAVE');
            expect(DialogService.alert).not.toHaveBeenCalled();
            expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
            done();
          });
        $rootScope.$digest();
      });

      it('discards the changes when the dialog resolves with "DISCARD"', (done) => {
        spyOn(ComponentEditor, 'discardChanges');
        spyOn(ComponentEditor, 'save');
        DialogService.show.and.returnValue($q.resolve('DISCARD'));

        ComponentEditor.confirmSaveOrDiscardChanges(true)
          .then((action) => {
            expect(action).toBe('DISCARD');
            expect(ComponentEditor.discardChanges).toHaveBeenCalled();
            expect(ComponentEditor.save).not.toHaveBeenCalled();
            done();
          });
        $rootScope.$digest();
      });
    });

    describe('with invalid data', () => {
      beforeEach(() => {
        const alert = jasmine.createSpyObj('alert', ['ok', 'textContent']);
        alert.textContent.and.returnValue(alert);
        alert.ok.and.returnValue(alert);
        DialogService.alert.and.returnValue(alert);
      });

      it('shows an alert when the dialog resolved with "SAVE" and neither saves nor redraws the component', (done) => {
        spyOn(ComponentEditor, 'save');
        spyOn(PageStructureService, 'renderComponent');
        DialogService.show.and.returnValues($q.resolve('SAVE'), $q.resolve());

        ComponentEditor.confirmSaveOrDiscardChanges(false)
          .catch(() => {
            expect(DialogService.alert).toHaveBeenCalled();
            expect(ComponentEditor.save).not.toHaveBeenCalled();
            expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
            done();
          });
        $rootScope.$digest();
      });

      it('discards the changes when the dialog resolves with "DISCARD" and does not save', (done) => {
        spyOn(ComponentEditor, 'discardChanges');
        spyOn(ComponentEditor, 'save');
        DialogService.show.and.returnValue($q.resolve('DISCARD'));

        ComponentEditor.confirmSaveOrDiscardChanges(false)
          .then(() => {
            expect(ComponentEditor.discardChanges).toHaveBeenCalled();
            expect(ComponentEditor.save).not.toHaveBeenCalled();
            done();
          });
        $rootScope.$digest();
      });
    });
  });
});
