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
  let CKEditor;
  let config;
  let editor;
  let $q;
  let model;

  const $element = angular.element('<div><textarea></textarea></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_,
            $rootScope,
            _CKEditorService_,
            _ConfigService_,
            _$q_) => {
      $componentController = _$componentController_;
      $scope = $rootScope.$new();
      CKEditorService = _CKEditorService_;
      ConfigService = _ConfigService_;
      $q = _$q_;
    });

    config = {};

    CKEditor = jasmine.createSpyObj('CKEditor', ['replace']);
    editor = jasmine.createSpyObj('editor', [
      'destroy',
      'getData',
      'on',
      'setData',
    ]);
    CKEditor.replace.and.returnValue(editor);

    model = jasmine.createSpyObj('model', [
      '$setViewValue',
      '$viewValue',
    ]);

    model.$setViewValue.and.callFake((html) => {
      model.$viewValue = html;
    });

    spyOn(CKEditorService, 'loadCKEditor').and.returnValue($q.resolve(CKEditor));

    const onFocus = jasmine.createSpy('onFocus');
    const onBlur = jasmine.createSpy('onBlur');

    $ctrl = $componentController('ckeditor', {
      $scope,
      $element,
      CKEditorService,
    }, {
      model,
      name: 'TestField',
      ariaLabel: 'TestAriaLabel',
      config,
      onFocus,
      onBlur,
    });

    model.$viewValue = '<p>initial value</p>';
  });

  function init() {
    $ctrl.$onInit();
    $scope.$apply();
  }

  function getEventListener(event) {
    expect(editor.on).toHaveBeenCalledWith(event, jasmine.any(Function));
    const onCall = editor.on.calls.all().find(call => call.args[0] === event);
    return onCall.args[1];
  }

  it('initializes the component', () => {
    init();
    expect($ctrl.model.$viewValue).toEqual = '<p>initial value</p>';
    expect($ctrl.name).toEqual('TestField');
    expect($ctrl.ariaLabel).toEqual('TestAriaLabel');
    expect($ctrl.config).toEqual(config);
    expect($ctrl.onFocus).toBeDefined();
    expect($ctrl.onBlur).toBeDefined();
  });

  it('uses the current language', () => {
    ConfigService.locale = 'fr';
    init();
    expect(CKEditor.replace).toHaveBeenCalledWith(jasmine.any(Object), {
      language: 'fr',
    });
  });

  it('update ckeditor value on change', () => {
    init();
    const onChange = getEventListener('change');
    const newValue = '<p>changed</p>';
    editor.getData.and.returnValue(newValue);

    onChange();
    $scope.$apply();
    expect(model.$viewValue).toBe(newValue);
  });

  it('ckeditor is focused', () => {
    init();
    const onEditorFocus = getEventListener('focus');
    onEditorFocus();
    expect($ctrl.onFocus).toHaveBeenCalled();
  });

  it('ckeditor is blurred', () => {
    init();
    const onEditorBlur = getEventListener('blur');
    onEditorBlur();
    expect($ctrl.onBlur).toHaveBeenCalled();
  });

  it('destroys the editor once the scope is destroyed', () => {
    init();
    $scope.$destroy();
    expect(editor.destroy).toHaveBeenCalled();
  });
});
