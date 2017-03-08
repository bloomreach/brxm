/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('ckeditor directive', () => {
  let $compile;
  let scope;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$compile_, $rootScope) => {
      $compile = _$compile_;
      scope = $rootScope.$new();
    });
  });

  describe('with a model', () => {
    let CKEDITOR;
    let editor;
    let ConfigService;

    beforeEach(() => {
      CKEDITOR = jasmine.createSpyObj('CKEDITOR', ['replace']);
      editor = jasmine.createSpyObj('editor', [
        'destroy',
        'getData',
        'on',
        'setData',
      ]);
      CKEDITOR.replace.and.returnValue(editor);

      inject((_ConfigService_, $window) => {
        ConfigService = _ConfigService_;
        $window.CKEDITOR = CKEDITOR;
      });

      scope.value = '<p>text</p>';
    });

    function compile() {
      const compiledElement = $compile('<textarea ckeditor ng-model="value"></textarea>')(scope);
      scope.$digest();
      return compiledElement;
    }

    function getEventListener(event) {
      expect(editor.on).toHaveBeenCalledWith(event, jasmine.any(Function));
      const onCall = editor.on.calls.all().find(call => call.args[0] === event);
      return onCall.args[1];
    }

    it('renders an editor for the model value', () => {
      compile();
      expect(editor.setData).toHaveBeenCalledWith(scope.value);
    });

    it('uses the current locale', () => {
      ConfigService.locale = 'nl';
      compile();
      const editorConfig = CKEDITOR.replace.calls.mostRecent().args[1];
      expect(editorConfig.language).toBe('nl');
    });

    it('updates the model value when the editor has changes', () => {
      compile();
      const onChange = getEventListener('change');

      const newValue = '<p>changed</p>';
      editor.getData.and.returnValue(newValue);

      onChange();
      scope.$digest();

      expect(scope.value).toBe(newValue);
    });

    it('triggers focus handlers on the element when the editor is focused', () => {
      const compiledElement = compile();
      const onEditorFocus = getEventListener('focus');

      const onElementFocus = jasmine.createSpy('onElementFocus');
      compiledElement.on('focus', onElementFocus);

      onEditorFocus();

      expect(onElementFocus).toHaveBeenCalled();
    });

    it('triggers blur handlers on the element when the editor is blurred', () => {
      const compiledElement = compile();
      const onEditorBlur = getEventListener('blur');

      const onElementBlur = jasmine.createSpy('onElementBlur');
      compiledElement.on('blur', onElementBlur);

      onEditorBlur();

      expect(onElementBlur).toHaveBeenCalled();
    });

    it('destroys the editor the scope is destroyed', () => {
      compile();
      scope.$destroy();
      expect(editor.destroy).toHaveBeenCalled();
    });
  });

  describe('without a model', () => {
    it('does not compile', () => {
      expect(() => {
        $compile('<textarea ckeditor></textarea>')(scope);
      }).toThrow();
    });
  });
});
