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

describe('EditComponentService', () => {
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let ComponentEditor;
  let EditComponentService;
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ComponentEditor = jasmine.createSpyObj('ComponentEditor', ['getComponentName', 'open']);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', ['clearContext', 'setContext', 'setTitle', 'startLoading', 'stopLoading']);

    angular.mock.module(($provide) => {
      $provide.value('ComponentEditor', ComponentEditor);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _EditComponentService_,
      _RightSidePanelService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      EditComponentService = _EditComponentService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    spyOn($translate, 'instant').and.callThrough();
  });

  const testData = {
    component: {
      id: 'componentId',
      label: 'componentLabel',
    },
  };

  function editComponent(data) {
    ComponentEditor.open.and.returnValue($q.resolve());
    ComponentEditor.getComponentName.and.returnValue(data.component.label);

    EditComponentService.startEditing(data);
    $rootScope.$digest();
  }

  describe('start editing component properties', () => {
    it('clears the context label and sets the title to COMPONENT', () => {
      editComponent(testData);

      expect(RightSidePanelService.clearContext).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('COMPONENT');
    });

    it('starts the loading state of the right side panel', () => {
      editComponent(testData);

      expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    });

    it('opens the component editor', () => {
      editComponent(testData);

      expect(ComponentEditor.open).toHaveBeenCalledWith(testData);
    });

    it('stores the component id', () => {
      editComponent(testData);

      expect(EditComponentService.componentId).toBe(testData.component.id);
    });

    it('sets the context label to COMPONENT and the title label to the component name', () => {
      editComponent(testData);

      expect($translate.instant).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setContext).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith(testData.component.label);
    });

    it('stops the loading state of the right side panel', () => {
      editComponent(testData);

      expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    });
  });

  it('transitions to the parent state when editing is stopped', () => {
    spyOn($state, 'go');
    EditComponentService.stopEditing();
    expect($state.go).toHaveBeenCalledWith('^');
  });
});

