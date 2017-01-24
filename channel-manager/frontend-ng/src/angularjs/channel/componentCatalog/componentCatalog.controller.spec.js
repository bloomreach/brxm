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

describe('componentCatalogController', () => {
  let $translate;
  let MaskService;
  let ComponentCatalogService;
  let $ctrl;

  const component = {
    id: 'component',
    label: 'Test',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController, _$translate_, _MaskService_, _ComponentCatalogService_) => {
      $translate = _$translate_;
      MaskService = _MaskService_;
      ComponentCatalogService = _ComponentCatalogService_;

      $ctrl = $componentController('componentCatalog');
    });
  });

  it('sets the selected component', () => {
    spyOn(ComponentCatalogService, 'selectComponent');

    $ctrl.onSelect(component);

    expect($ctrl.selectedComponent).toBe(component);
    expect(ComponentCatalogService.selectComponent).toHaveBeenCalledWith(component);
  });

  it('returns the label of component as its label', () => {
    expect($ctrl.getComponentLabel(component)).toBe('Test');
  });

  describe('when component is selected', () => {
    beforeEach(() => {
      $ctrl.selectedComponent = component;
      MaskService.isMasked = true;
    });

    it('returns true when selected component matches and mask is on', () => {
      expect($ctrl.isComponentSelected(component)).toEqual(true);
    });

    it('returns false when mask is off', () => {
      MaskService.isMasked = false;
      expect($ctrl.isComponentSelected(component)).toEqual(false);
    });

    it('returns false when selected component does not match', () => {
      expect($ctrl.isComponentSelected({ other: 1 })).toEqual(false);
    });

    it('returns the adding-component-sentence as a component\'s label', () => {
      spyOn($translate, 'instant').and.callThrough();
      expect($ctrl.getComponentLabel(component)).toBe('ADDING_COMPONENT');
      expect($translate.instant).toHaveBeenCalledWith('ADDING_COMPONENT', { component: 'Test' });
    });
  });
});

