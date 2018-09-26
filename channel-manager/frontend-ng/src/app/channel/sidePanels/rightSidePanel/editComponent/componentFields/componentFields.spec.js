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

describe('ComponentFields', () => {
  let $componentController;
  let ComponentEditor;

  let component;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((_$componentController_, _ComponentEditor_) => {
      $componentController = _$componentController_;
      ComponentEditor = _ComponentEditor_;
    });

    form = {};

    component = $componentController('componentFields',
      {},
      { form },
    );
  });

  describe('valueChanged', () => {
    it('updates the preview when a value is changed and valid', () => {
      spyOn(ComponentEditor, 'updatePreview');
      form.$valid = true;

      component.valueChanged();

      expect(ComponentEditor.updatePreview).toHaveBeenCalled();
    });

    it('does not update the preview when a value is changed to something invalid', () => {
      spyOn(ComponentEditor, 'updatePreview');
      form.$valid = false;

      component.valueChanged();

      expect(ComponentEditor.updatePreview).not.toHaveBeenCalled();
    });
  });
});
