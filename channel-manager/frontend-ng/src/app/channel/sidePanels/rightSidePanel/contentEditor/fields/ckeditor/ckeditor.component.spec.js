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
  let SharedSpaceToolbarService;
  let CmsService;

  const $element = angular.element('<div><textarea></textarea></div>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.contentEditor.fields.ckeditor',
      ($translateProvider) => {
        $translateProvider.translations('fr', {});
      });

    inject((
      _$componentController_,
      $rootScope,
      _CKEditorService_,
      _ConfigService_,
      _DomService_,
      _SharedSpaceToolbarService_,
      _CmsService_,
      _$q_,
    ) => {
      $componentController = _$componentController_;
      $scope = $rootScope.$new();
      CKEditorService = _CKEditorService_;
      ConfigService = _ConfigService_;
      DomService = _DomService_;
      SharedSpaceToolbarService = _SharedSpaceToolbarService_;
      CmsService = _CmsService_;
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
      'focus',
      'execCommand',
    ]);
    CKEditor.replace.and.returnValue(editor);

    ngModel = jasmine.createSpyObj('ngModel', [
      '$setViewValue',
      '$viewValue',
    ]);

    ngModel.$setViewValue.and.callFake((html) => {
      ngModel.$viewValue = html;
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
    const editorConfig = CKEditor.replace.calls.mostRecent().args[1];
    expect(editorConfig.language).toEqual('fr');
  });

  it('adjusts the editor configuration without breaking tweaks by customers', () => {
    config.extraPlugins = 'smiley';
    config.removePlugins = 'justify';
    config.removeButtons = 'Bold';
    init();
    const editorConfig = CKEditor.replace.calls.mostRecent().args[1];
    expect(editorConfig.extraPlugins).toEqual('smiley,sharedspace,sourcedialog,autogrow');
    expect(editorConfig.removePlugins).toEqual('justify,sourcearea,resize,maximize');
    expect(editorConfig.removeButtons).toEqual('Bold,Source');
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

  it('calls the onFocus handler within $scope.$apply() when focused', () => {
    init();
    spyOn($scope, '$apply');

    const onEditorFocus = getEventListener('focus');
    onEditorFocus();

    expect($scope.$apply).toHaveBeenCalled();
    expect($ctrl.onFocus).not.toHaveBeenCalled();

    // call the function provided to $scope.$apply
    $scope.$apply.calls.mostRecent().args[0]();

    expect($ctrl.onFocus).toHaveBeenCalledWith({
      $event: {
        target: jasmine.any(Object),
        customFocus: jasmine.any(Function),
      },
    });
  });

  it('calls the onBlur handler within $scope.$apply() when blurred', () => {
    init();
    const $event = {};
    spyOn($scope, '$apply');

    const onEditorBlur = getEventListener('blur');
    onEditorBlur($event);

    expect($scope.$apply).toHaveBeenCalled();
    expect($ctrl.onFocus).not.toHaveBeenCalled();

    // call the function provided to $scope.$apply
    $scope.$apply.calls.mostRecent().args[0]();

    expect($ctrl.onBlur).toHaveBeenCalledWith({ $event });
  });

  it('destroys the editor once the scope is destroyed', () => {
    spyOn(SharedSpaceToolbarService, 'hideToolbar');

    init();
    $ctrl.$onDestroy();
    expect(editor.destroy).toHaveBeenCalled();
    expect(SharedSpaceToolbarService.hideToolbar).toHaveBeenCalled();
  });

  describe('link picker', () => {
    beforeEach(() => {
      init();
      $ctrl.config.hippopicker = {};
      $ctrl.config.hippopicker.internalLink = 'internalLinkTest';
    });

    it('should open link picker', () => {
      $ctrl._openLinkPicker();
      expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(true);
    });

    describe('callbacks', () => {
      let args = [];

      beforeEach(() => {
        spyOn(CmsService, 'publish');
        $ctrl._openLinkPicker();
        [args] = CmsService.publish.calls.allArgs();
      });

      it('should call success callback', () => {
        const linkObj = { link: 'link' };
        const successCallback = args[4];
        successCallback(linkObj);
        expect(editor.execCommand).toHaveBeenCalledWith('insertInternalLink', linkObj);
        expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(false);
      });

      it('should call fail (cancel) callback', () => {
        const failCallback = args[5];
        failCallback();
        expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(false);
        expect(editor.focus).toHaveBeenCalled();
      });
    });
  });

  describe('image picker', () => {
    beforeEach(() => {
      init();
      $ctrl.config.hippopicker = {};
      $ctrl.config.hippopicker.image = 'imageTest';
    });

    it('should open image picker', () => {
      $ctrl._openImageVariantPicker();
      expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(true);
    });

    describe('callbacks', () => {
      let args = [];

      beforeEach(() => {
        spyOn(CmsService, 'publish');
        $ctrl._openImageVariantPicker();
        [args] = CmsService.publish.calls.allArgs();
      });

      it('should call success callback', () => {
        const imageObj = { image: 'image', f_url: 'furl' };
        const successCallback = args[4];
        successCallback(imageObj);
        expect(editor.execCommand).toHaveBeenCalledWith('insertImage', imageObj);
        expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(false);
      });

      it('should call fail (cancel) callback', () => {
        const failCallback = args[5];
        failCallback();
        expect(SharedSpaceToolbarService.isToolbarPinned).toEqual(false);
        expect(editor.focus).toHaveBeenCalled();
      });
    });
  });
});
