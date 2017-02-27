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
      $compile('<textarea ckeditor ng-model="value"></textarea>')(scope);
      scope.$digest();
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
      expect(editor.on).toHaveBeenCalledWith('change', jasmine.any(Function));

      const onChangeCallback = editor.on.calls.mostRecent().args[1];

      const newValue = '<p>changed</p>';
      editor.getData.and.returnValue(newValue);

      onChangeCallback();
      scope.$digest();

      expect(scope.value).toBe(newValue);
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
