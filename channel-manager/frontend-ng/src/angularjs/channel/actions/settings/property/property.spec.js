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

describe('ChannelProperty', () => {
  'use strict';

  let $rootScope;
  let $compile;
  let $log;
  let ConfigService;
  let channelInfoDescription;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$rootScope_, _$compile_, _$log_, _ConfigService_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $log = _$log_;
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
      },
    };
  });

  function compileDirectiveAndGetController(field = 'testField') {
    const $scope = $rootScope.$new();
    $scope.field = field;
    $scope.value = 'testValue';
    $scope.data = channelInfoDescription;

    const $element = angular.element(`
      <div channel-property="{{field}}" channel-property-value="value" channel-properties-data="data"></div>
    `);
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('channel-property');
  }

  it('initializes correctly', () => {
    const ChannelPropertyCtrl = compileDirectiveAndGetController();

    expect(ChannelPropertyCtrl.value).toBe('testValue');
    expect(ChannelPropertyCtrl.qaClass).toBe('qa-field-testField');
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
});
