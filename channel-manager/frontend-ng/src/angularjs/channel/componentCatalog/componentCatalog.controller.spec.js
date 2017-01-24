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
  let MaskService;
  let ComponentCatalogService;
  let $ctrl;

  const component = { id: 'component' };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController, _MaskService_, _ComponentCatalogService_) => {
      MaskService = _MaskService_;
      ComponentCatalogService = _ComponentCatalogService_;

      $ctrl = $componentController('componentCatalog', {
        MaskService,
        ComponentCatalogService,
      });
    });

    spyOn(ComponentCatalogService, 'getSelectedComponent').and.returnValue(component);
  });

  it('should set the selected component', () => {
    spyOn(ComponentCatalogService, 'selectComponent');

    $ctrl.onSelect(component);

    expect(ComponentCatalogService.selectComponent).toHaveBeenCalledWith(component);
  });

  describe('when component is selected', () => {
    it('should return true when selected component matches and mask is on', () => {
      MaskService.isMasked = true;

      expect($ctrl.isComponentSelected(component)).toEqual(true);
    });

    it('should return false when mask is off', () => {
      MaskService.isMasked = false;

      expect($ctrl.isComponentSelected(component)).toEqual(false);
    });

    it('should return false when selected component does not match', () => {
      MaskService.isMasked = true;

      expect($ctrl.isComponentSelected({ other: 1 })).toEqual(false);
    });
  });
});

