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

describe('CKEditor Component', () => {
  let $componentController;

  let $ctrl;
  let $scope;
  let CKEditorService;
  let ConfigService;
  let DomService;
  let CKEditor;
  let config;
  let editor;
  let $q;
  let ngModel;
  let fieldObject;

  const $element = angular.element('<div><textarea></textarea></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm', ($translateProvider) => {
      $translateProvider.translations('fr', {});
    });

    inject((_$componentController_,
            $rootScope,
            _CKEditorService_,
            _ConfigService_,
            _DomService_,
            _$q_) => {
      $componentController = _$componentController_;
      $scope = $rootScope.$new();
      CKEditorService = _CKEditorService_;
      ConfigService = _ConfigService_;
      DomService = _DomService_;
      $q = _$q_;
    });

    config = {};

    CKEditor = jasmine.createSpyObj('CKEditor', ['replace']);
    editor = jasmine.createSpyObj('editor', [
      'destroy',
      'getData',
      'on',
      'setData',
      'getSnapshot',
    ]);
    CKEditor.replace.and.returnValue(editor);

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setViewValue',
      '$viewValue',
    ]);

    ngModel.$setViewValue.and.callFake((html) => {
      ngModel.$viewValue = html;
    });

    fieldObject = jasmine.createSpyObj('fieldObject', [
      '$setValidity',
      '$valid',
      '$invalid',
      '$error',
    ]);
    fieldObject.$error = {};


    fieldObject.$setValidity.and.callFake((field, isValid) => {
      fieldObject.$error[field] = isValid;
      fieldObject.$valid = isValid;
      fieldObject.$invalid = !isValid;
    });

    spyOn(CKEditorService, 'loadCKEditor').and.returnValue($q.resolve(CKEditor));

    const onFocus = jasmine.createSpy('onFocus');
    const onBlur = jasmine.createSpy('onBlur');

    $ctrl = $componentController('ckeditor', {
      $scope,
      $element,
      CKEditorService,
    }, {
      ngModel,
      name: 'TestField',
      ariaLabel: 'TestAriaLabel',
      config,
      onFocus,
      onBlur,
    });

    ngModel.$viewValue = '<p>initial value</p>';
    $ctrl.fieldObject = fieldObject;
  });

  function getEventListener(event) {
    expect(editor.on).toHaveBeenCalledWith(event, jasmine.any(Function));
    const onCall = editor.on.calls.all().find(call => call.args[0] === event);
    return onCall.args[1];
  }

  function init() {
    $ctrl.$onInit();
    $scope.$apply();
    const instanceReady = getEventListener('instanceReady');
    instanceReady();
  }

  it('initializes the component', () => {
    init();
    expect($ctrl.ngModel.$viewValue).toEqual = '<p>initial value</p>';
    expect($ctrl.name).toEqual('TestField');
    expect($ctrl.ariaLabel).toEqual('TestAriaLabel');
    expect($ctrl.config).toEqual(config);
    expect($ctrl.onFocus).toBeDefined();
    expect($ctrl.onBlur).toBeDefined();
    expect($ctrl.textAreaElement).toBeDefined();
  });

  it('should apply CSS specified as string to the editor', () => {
    init();

    spyOn(DomService, 'addCssLinks');
    config.contentsCss = 'hippocontents.css';
    $ctrl._applyEditorCSS(config);
    expect(DomService.addCssLinks).toHaveBeenCalled();
    expect(Array.isArray(config.contentsCss)).toBe(true);
    expect(config.contentsCss.length).toEqual(1);
    expect(config.contentsCss[0]).toEqual('../../hippocontents.css');
  });

  it('should apply CSS specified as an array to the editor', () => {
    init();

    spyOn(DomService, 'addCssLinks');
    config.contentsCss = ['foo.css', 'bar.css'];
    $ctrl._applyEditorCSS(config);
    expect(DomService.addCssLinks).toHaveBeenCalled();
    expect(Array.isArray(config.contentsCss)).toBe(true);
    expect(config.contentsCss.length).toEqual(2);
    expect(config.contentsCss[0]).toEqual('../../foo.css');
    expect(config.contentsCss[1]).toEqual('../../bar.css');
  });

  it('uses the current language', () => {
    ConfigService.locale = 'fr';
    init();
    expect(CKEditor.replace).toHaveBeenCalled();
  });

  it('updates the editor data when the model changes', () => {
    init();

    ngModel.$viewValue = '<p>changed</p>';
    ngModel.$render();

    expect(editor.setData).toHaveBeenCalledWith('<p>changed</p>');
  });

  it('updates the model data when the editor changes', () => {
    init();
    const onChange = getEventListener('change');
    const newValue = '<p>changed</p>';
    editor.getData.and.returnValue(newValue);

    onChange();
    $scope.$apply();
    expect(ngModel.$viewValue).toBe(newValue);
  });

  it('does not update the model data while the component is processing a model change', () => {
    init();

    const onChange = getEventListener('change');
    editor.setData.and.callFake(onChange);

    ngModel.$viewValue = '<p>changed</p>';
    ngModel.$render();

    expect(ngModel.$setViewValue).not.toHaveBeenCalled();
  });

  it('calls the onFocus handler when focused', () => {
    init();
    const onEditorFocus = getEventListener('focus');
    onEditorFocus();
    expect($ctrl.onFocus).toHaveBeenCalledWith({
      $event: {
        target: jasmine.any(Object),
        customFocus: jasmine.any(Function),
      },
    });
  });

  it('destroys the editor once the scope is destroyed', () => {
    init();
    $ctrl.$onDestroy();
    expect(editor.destroy).toHaveBeenCalled();
  });

  describe('CKEditor field validation', () => {
    beforeEach(() => {
      init();
      $ctrl.fieldObject.$valid = true;
      $ctrl.fieldObject.$invalid = false;
      $ctrl.fieldObject.$error = {};

      spyOn($ctrl, '_getRawValue');
    });

    afterEach(() => {
      $ctrl.fieldObject.$setValidity.calls.reset();
    });

    it('validates CKEditor when field is required and value is valid', () => {
      $ctrl.isRequired = true;
      $ctrl._getRawValue.and.returnValue('Valid value');

      $ctrl._validate();
      expect($ctrl.fieldObject.$valid).toEqual(true);
    });

    it('validates CKEditor when field is required and value is completely empty', () => {
      $ctrl.isRequired = true;
      $ctrl._getRawValue.and.returnValue('');

      $ctrl._validate();
      expect($ctrl.fieldObject.$setValidity).toHaveBeenCalledWith('required', false);
      expect($ctrl.fieldObject.$invalid).toEqual(true);
    });

    it('validates CKEditor when field is required and value is pure whitespaces', () => {
      $ctrl.isRequired = true;
      $ctrl._getRawValue.and.returnValue($('    \n\n   \n\r\t ').text().trim());

      $ctrl._validate();
      expect($ctrl.fieldObject.$setValidity).toHaveBeenCalledWith('required', false);
      expect($ctrl.fieldObject.$invalid).toEqual(true);
    });
  });
});
