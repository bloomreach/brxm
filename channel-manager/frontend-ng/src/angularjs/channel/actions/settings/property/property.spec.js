/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelProperty', () => {
  let $rootScope;
  let $compile;
  let $log;
  let ChannelService;
  let ConfigService;
  let channelInfoDescription;
  let $element;
  let $scope;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$log_, _ChannelService_, _ConfigService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $log = _$log_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
    });

    channelInfoDescription = {
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
        'testField.help': 'Test Field help text',
      },
    };
  });

  function compileDirectiveAndGetController(field = 'testField', value = 'testValue') {
    $scope = $rootScope.$new();
    $scope.field = field;
    $scope.value = value;
    $scope.fieldError = { };
    $scope.data = channelInfoDescription;

    $element = angular.element(`
      <div channel-property="{{field}}"
           channel-property-value="value"
           channel-properties-error="fieldError"
           channel-properties-data="data"></div>
    `);
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channelProperty');
  }

  it('initializes correctly', () => {
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    expect(ChannelPropertyCtrl.value).toBe('testValue');
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-testField');
    expect(ChannelPropertyCtrl.help).toBe('Test Field help text');
  });

  it('applies a fall-back strategy when determining the label', () => {
    let ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.label).toBe('Test Field');

    ChannelPropertyCtrl = compileDirectiveAndGetController('unknownField');
    expect(ChannelPropertyCtrl.label).toBe('unknownField');
  });

  it('generates valid QA classnames', () => {
    let ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-testField');

    ChannelPropertyCtrl = compileDirectiveAndGetController('one space');
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-one-space');

    ChannelPropertyCtrl = compileDirectiveAndGetController('two  spaces');
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-two-spaces');

    ChannelPropertyCtrl = compileDirectiveAndGetController('double " quote');
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-double-quote');
  });

  it('determines the type of a field', () => {
    // Known annotation
    let ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('DropDownList');

    // Two annotations (ignore second)
    spyOn($log, 'warn');
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [{ type: 'DropDownList' }, { type: 'CheckBox' }],
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('DropDownList');
    expect($log.warn).toHaveBeenCalled();

    // Unknown annotation
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [{ type: 'UnknownType' }],
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('InputBox');

    // No annotation
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
        annotations: [],
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('InputBox');

    // No annotations
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'NOT_A_BOOLEAN',
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('InputBox');

    // No annotation, but boolean type
    channelInfoDescription.propertyDefinitions = {
      testField: {
        valueType: 'BOOLEAN',
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.type).toBe('CheckBox');
  });

  it('applies a sanity check on drop-down fields', () => {
    let ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.getDropDownListValues()).toEqual(['small', 'medium', 'large']);

    // Wrong annotation type
    let data = {
      propertyDefinitions: {
        testField: {
          valueType: 'NOT_A_BOOLEAN',
          annotations: [{ type: 'CheckBox' }],
        },
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController({ data });
    expect(ChannelPropertyCtrl.getDropDownListValues()).toEqual([]);

    // No annotations
    data = {
      propertyDefinitions: {
        testField: {
          valueType: 'NOT_A_BOOLEAN',
        },
      },
    };
    ChannelPropertyCtrl = compileDirectiveAndGetController({ data });
    expect(ChannelPropertyCtrl.getDropDownListValues()).toEqual([]);
  });

  it('enters read-only mode if the channel is locked by someone else', () => {
    ConfigService.cmsUser = 'admin';
    channelInfoDescription.lockedBy = 'tester';
    let ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.readOnly).toBe(true);

    channelInfoDescription.lockedBy = 'admin';
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.readOnly).toBe(false);

    delete channelInfoDescription.lockedBy;
    ChannelPropertyCtrl = compileDirectiveAndGetController();
    expect(ChannelPropertyCtrl.readOnly).toBeFalsy();
  });

  it('can open a link picker for JcrPath fields', () => {
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
    spyOn(window.APP_TO_CMS, 'publish');
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    ChannelPropertyCtrl.showPicker();

    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-picker', 'testField', 'testValue', {
      configuration: 'testPickerConfiguration',
      initialPath: 'testInitialPath',
      isRelativePath: 'testIsRelative',
      remembersLastVisited: 'testRemembersLastVisited',
      rootPath: 'testRootPath',
      selectableNodeTypes: ['testNodeType'],
    });
  });

  it('uses a channel\'s content root path as the default value of the picker\'s root path', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'JcrPath' }],
      },
    };
    spyOn(window.APP_TO_CMS, 'publish');
    spyOn(ChannelService, 'getContentRootPath').and.returnValue('testChannelContentRootPath');
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    ChannelPropertyCtrl.showPicker();

    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-picker', 'testField', 'testValue', {
      configuration: undefined,
      initialPath: undefined,
      isRelativePath: undefined,
      remembersLastVisited: undefined,
      rootPath: 'testChannelContentRootPath',
      selectableNodeTypes: undefined,
    });
  });

  it('updates the value to the picked path', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'JcrPath' }],
      },
    };
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    window.CMS_TO_APP.publish('picked', 'testField', '/picked/path');

    expect(ChannelPropertyCtrl.value).toEqual('/picked/path');
  });

  it('ignores the picked value of other JcrPath fields', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'JcrPath' }],
      },
    };
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    window.CMS_TO_APP.publish('picked', 'otherField', '/picked/path');

    expect(ChannelPropertyCtrl.value).toEqual('testValue');
  });

  it('does not update the picked value anymore when the scope has been destroyed', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'JcrPath' }],
      },
    };
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    window.CMS_TO_APP.publish('picked', 'testField', '/picked/path/one');
    $scope.$destroy();
    window.CMS_TO_APP.publish('picked', 'testField', '/picked/path/two');
    expect(ChannelPropertyCtrl.value).toEqual('/picked/path/one');
  });

  it('by default renders the hippogallery:thumbnail variant for an ImageSetPath field', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'ImageSetPath' }],
      },
    };
    ConfigService.cmsLocation = $j('<a href="http://localhost:8080/cms?1&path=/content/documents/example"></a>')[0];

    compileDirectiveAndGetController('testField', '/content/gallery/example/image.png');

    expect($element.find('img').attr('src')).toEqual('http://localhost:8080/cms/binaries/content/gallery/example/image.png/image.png/hippogallery:thumbnail');
  });

  it('can render a custom image variant for an ImageSetPath field', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{
          type: 'ImageSetPath',
          previewVariant: 'example:customimagevariant',
        }],
      },
    };
    ConfigService.cmsLocation = $j('<a href="http://localhost:8080/cms?1&path=/content/documents/example"></a>')[0];

    compileDirectiveAndGetController('testField', '/content/gallery/example/image.png');

    expect($element.find('img').attr('src')).toEqual('http://localhost:8080/cms/binaries/content/gallery/example/image.png/image.png/example:customimagevariant');
  });

  it('can render an ImageSetPath field on a CMS location without a context path', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{ type: 'ImageSetPath' }],
      },
    };
    ConfigService.cmsLocation = $j('<a href="https://cms.example.com/?1&path=/content/documents/example"></a>')[0];

    compileDirectiveAndGetController('testField', '/content/gallery/example/image.png');

    expect($element.find('img').attr('src')).toEqual('https://cms.example.com/binaries/content/gallery/example/image.png/image.png/hippogallery:thumbnail');
  });

  it('can open a link picker for ImageSetPath fields', () => {
    channelInfoDescription.propertyDefinitions = {
      testField: {
        annotations: [{
          type: 'ImageSetPath',
          previewVariant: 'example:customimagevariant',
          pickerConfiguration: 'testPickerConfiguration',
          pickerInitialPath: 'testInitialPath',
          pickerRemembersLastVisited: 'testRemembersLastVisited',
          pickerSelectableNodeTypes: ['testNodeType'],
        }],
      },
    };
    ConfigService.cmsLocation = $j('<a href="https://www.example.com/cms?1&path=/content/documents/example"></a>')[0];
    spyOn(window.APP_TO_CMS, 'publish');
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    ChannelPropertyCtrl.showPicker();

    expect(window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-picker', 'testField', 'testValue', {
      configuration: 'testPickerConfiguration',
      initialPath: 'testInitialPath',
      isRelativePath: undefined,
      remembersLastVisited: 'testRemembersLastVisited',
      rootPath: undefined,
      selectableNodeTypes: ['testNodeType'],
    });
  });
});
