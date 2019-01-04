/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

describe('Property field component', () => {
  let $componentController;
  let $ctrl;
  let $log;
  let $rootScope;
  let $q;
  let ChannelService;
  let ConfigService;
  let PathService;
  let PickerService;

  const originalChannelInfoDescription = {
    propertyDefinitions: {
      testField: {
        isRequired: false,
        defaultValue: '',
        name: 'unused',
        valueType: 'STRING',
        annotations: [
          {
            type: 'DropDownList',
            value: ['small', 'medium', 'large'],
          },
        ],
      },
    },
    i18nResources: {
      testField: 'Test Field',
      'testField.hint': 'Test Field hint text',
    },
  };

  let channelInfoDescription;

  function initComponentController(field = 'testField', value = 'testValue', readOnly = false) {
    $ctrl = $componentController('propertyField', {}, {
      error: {},
      field,
      info: channelInfoDescription,
      readOnly,
      value,
    });

    $ctrl.$onInit();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('PickerService', PickerService);
    });

    inject((
      _$componentController_,
      _$log_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _ConfigService_,
      _PathService_,
    ) => {
      $componentController = _$componentController_;
      $log = _$log_;
      $rootScope = _$rootScope_;
      $q = _$q_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      PathService = _PathService_;
    });

    channelInfoDescription = angular.copy(originalChannelInfoDescription);
  });

  it('initializes correctly', () => {
    initComponentController();

    expect($ctrl.value).toBe('testValue');
    expect($ctrl.qaClass).toBe('qa-field-testField');
    expect($ctrl.hint).toBe('Test Field hint text');
  });

  it('applies a fall-back strategy when determining the label', () => {
    initComponentController();
    expect($ctrl.label).toBe('Test Field');

    initComponentController('unknownField');
    expect($ctrl.label).toBe('unknownField');
  });

  it('generates valid QA classnames', () => {
    initComponentController();
    expect($ctrl.qaClass).toBe('qa-field-testField');

    initComponentController('one space');
    expect($ctrl.qaClass).toBe('qa-field-one-space');

    initComponentController('two  spaces');
    expect($ctrl.qaClass).toBe('qa-field-two-spaces');

    initComponentController('double " quote');
    expect($ctrl.qaClass).toBe('qa-field-double-quote');
  });

  it('determines the type of a field', () => {
    // Known annotation
    initComponentController();
    expect($ctrl.type).toBe('DropDownList');

    // Two annotations (ignore second)
    spyOn($log, 'warn');
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [{ type: 'DropDownList' }, { type: 'CheckBox' }],
      },
    };
    initComponentController();
    expect($ctrl.type).toBe('DropDownList');
    expect($log.warn).toHaveBeenCalled();

    // Unknown annotation
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [{ type: 'UnknownType' }],
      },
    };
    initComponentController();
    expect($ctrl.type).toBe('InputBox');

    // No annotation
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [],
      },
    };
    initComponentController();
    expect($ctrl.type).toBe('InputBox');

    // No annotations
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
      },
    };
    initComponentController();
    expect($ctrl.type).toBe('InputBox');

    // No annotation, but boolean type
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'BOOLEAN',
      },
    };
    initComponentController();
    expect($ctrl.type).toBe('CheckBox');
  });

  it('applies a sanity check on drop-down fields', () => {
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([{ value: 'small', displayName: 'small' },
      { value: 'medium', displayName: 'medium' },
      { value: 'large', displayName: 'large' }]);

    // Wrong annotation type
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [{ type: 'CheckBox' }],
      },
    };
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([]);

    // No annotations
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
      },
    };
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([]);
  });

  it('shows keys as display values in drop-downs if there are no translations available', () => {
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([{ value: 'small', displayName: 'small' },
      { value: 'medium', displayName: 'medium' },
      { value: 'large', displayName: 'large' }]);
  });

  it('shows translated display values in drop-downs for properties files', () => {
    channelInfoDescription.i18nResources = {
      testField: 'Test Field',
      'testField.hint': 'Test Field hint text',
      'testField/small': 'Small',
      'testField/medium': 'Medium',
      'testField/large': 'Large',
    };
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([{ value: 'small', displayName: 'Small' },
      { value: 'medium', displayName: 'Medium' },
      { value: 'large', displayName: 'Large' }]);
  });

  it('shows translated display values in drop-downs for repository resource bundles', () => {
    channelInfoDescription.i18nResources = {
      testField: 'Test Field',
      'testField.hint': 'Test Field hint text',
      'testField#small': 'Small',
      'testField#medium': 'Medium',
      'testField#large': 'Large',
    };
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([{ value: 'small', displayName: 'Small' },
      { value: 'medium', displayName: 'Medium' },
      { value: 'large', displayName: 'Large' }]);
  });

  it('shows translated display values in drop-downs for value list providers', () => {
    channelInfoDescription.i18nResources = {
      testField: 'Test Field',
      'testField.hint': 'Test Field hint text',
      'testField.small': 'Small',
      'testField.medium': 'Medium',
      'testField.large': 'Large',
    };
    initComponentController();
    expect($ctrl.dropDownValues).toEqual([{ value: 'small', displayName: 'Small' },
      { value: 'medium', displayName: 'Medium' },
      { value: 'large', displayName: 'Large' }]);
  });

  it('enters read-only mode if the channel is locked by someone else even if the channel is editable', () => {
    initComponentController('testField', 'testValue', true);
    expect($ctrl.readOnly).toBe(true);
  });

  describe('showPathPicker', () => {
    beforeEach(() => {
      PickerService.pickPath.and.returnValue($q.resolve({ path: '/picked/path' }));
    });

    it('can open a link picker', () => {
      channelInfoDescription.propertyDefinitions = {
        testField: {
          annotations: [{
            type: 'JcrPath',
            pickerConfiguration: 'testPickerConfiguration',
            pickerInitialPath: 'testInitialPath',
            isRelative: 'testIsRelative',
            pickerRemembersLastVisited: 'testRemembersLastVisited',
            pickerRootPath: 'testRootPath',
            pickerSelectableNodeTypes: ['testNodeType'],
          }],
        },
      };
      initComponentController();

      $ctrl.showPathPicker();
      $rootScope.$digest();

      expect(PickerService.pickPath).toHaveBeenCalledWith({
        configuration: 'testPickerConfiguration',
        initialPath: 'testInitialPath',
        isRelativePath: 'testIsRelative',
        remembersLastVisited: 'testRemembersLastVisited',
        rootPath: 'testRootPath',
        selectableNodeTypes: ['testNodeType'],
      }, 'testValue');
    });

    it('uses a channel\'s content root path as the default value of the picker\'s root path', () => {
      channelInfoDescription.propertyDefinitions = {
        testField: {
          annotations: [{ type: 'JcrPath' }],
        },
      };
      spyOn(ChannelService, 'getContentRootPath').and.returnValue('testChannelContentRootPath');
      initComponentController();

      $ctrl.showPathPicker();
      $rootScope.$digest();

      expect(PickerService.pickPath).toHaveBeenCalledWith({
        configuration: undefined,
        initialPath: undefined,
        isRelativePath: undefined,
        remembersLastVisited: undefined,
        rootPath: 'testChannelContentRootPath',
        selectableNodeTypes: undefined,
      }, 'testValue');
    });

    it('updates the value to the picked path', (done) => {
      channelInfoDescription.propertyDefinitions = {
        testField: {
          annotations: [{ type: 'JcrPath' }],
        },
      };
      initComponentController();

      $ctrl.showPathPicker()
        .then(() => {
          expect($ctrl.value).toEqual('/picked/path');
          done();
        });
      $rootScope.$digest();
    });
  });

  it('by default renders the hippogallery:thumbnail variant for an ImageSetPath field', () => {
    spyOn(PathService, 'baseName');
    ConfigService.cmsLocation = {
      pathname: 'testpath',
      host: 'testhost',
      protocol: 'testprotocol',
    };
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'ImageSetPath' }],
      },
    };

    initComponentController();

    expect($ctrl.getImageVariantPath().includes('hippogallery:thumbnail')).toBe(true);
  });

  it('can render a custom image variant for an ImageSetPath field', () => {
    spyOn(PathService, 'baseName');
    ConfigService.cmsLocation = {
      pathname: 'testpath',
      host: 'testhost',
      protocol: 'testprotocol',
    };
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{
          type: 'ImageSetPath',
          previewVariant: 'example:customimagevariant',
        }],
      },
    };

    initComponentController();

    expect($ctrl.getImageVariantPath().includes('example:customimagevariant')).toBe(true);
  });

  it('can render an ImageSetPath field on a CMS location without a context path', () => {
    spyOn(PathService, 'baseName');
    ConfigService.cmsLocation = {
      pathname: '',
      host: 'testhost',
      protocol: 'testprotocol',
    };
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'ImageSetPath' }],
      },
    };

    initComponentController();

    expect($ctrl.getImageVariantPath().includes('testpath')).toBe(false);
  });
});
