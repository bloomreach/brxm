/*
 * Copyright 2020 Bloomreach
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

describe('editPageUnavailableComponent', () => {
  let $componentController;
  let $ctrl;
  let $uiRouterGlobals;

  let EditContentService;
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$componentController_,
      _$uiRouterGlobals_,
      _EditContentService_,
      _RightSidePanelService_,
    ) => {
      $componentController = _$componentController_;
      $uiRouterGlobals = _$uiRouterGlobals_;
      EditContentService = _EditContentService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    $uiRouterGlobals.params = { documentId: 'test-document-id', title: 'test-title' };

    spyOn(RightSidePanelService, 'setTitle');

    $ctrl = $componentController('editPageUnavailable');
    $ctrl.$onInit();
  });

  it('should set the title', () => {
    expect(RightSidePanelService.setTitle).toHaveBeenCalled();
  });

  it('closes the content service', () => {
    spyOn(EditContentService, 'stopEditing');
    $ctrl.close();
    expect(EditContentService.stopEditing).toHaveBeenCalled();
  });
});
