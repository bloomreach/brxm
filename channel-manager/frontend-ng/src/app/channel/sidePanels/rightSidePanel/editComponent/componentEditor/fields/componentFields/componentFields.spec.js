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

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent.componentEditor');

    inject((_$componentController_, _ComponentEditor_) => {
      $componentController = _$componentController_;
      ComponentEditor = _ComponentEditor_;
    });

    component = $componentController('componentFields');
    component.$onInit();
  });

  it('triggers valueChanged() on the ComponentEditor when a value is changed', () => {
    spyOn(ComponentEditor, 'valueChanged');

    component.valueChanged();

    expect(ComponentEditor.valueChanged).toHaveBeenCalled();
  });
});
