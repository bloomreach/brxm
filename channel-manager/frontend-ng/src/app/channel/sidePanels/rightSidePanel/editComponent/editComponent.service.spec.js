/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  let $log;
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let $window;

  let ChannelService;
  let ComponentEditor;
  let EditComponentService;
  let MaskService;
  let RightSidePanelService;

  let mockComponent;

  const testData = {
    channel: {
      contextPath: 'channel.contextPath',
      mountId: 'channel.mountId',
    },
    component: {
      id: 'component.id',
      label: 'component.label',
      lastModified: 'component.lastModified',
      variant: 'component.renderVariant',
    },
    container: {
      isDisabled: 'container.disabled',
      isInherited: 'container.inherited',
      id: 'container.uuid',
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    ComponentEditor = jasmine.createSpyObj('ComponentEditor', [
      'getComponentName', 'kill', 'open', 'updatePreview',
    ]);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
      'clearContext', 'setContext', 'setTitle', 'startLoading', 'stopLoading',
    ]);
    mockComponent = jasmine.createSpyObj('ComponentElement', [
      'getId', 'getLabel', 'getLastModified', 'getRenderVariant',
    ]);
    mockComponent.container = jasmine.createSpyObj('ContainerElement', [
      'isDisabled', 'isInherited', 'getId',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('ComponentEditor', ComponentEditor);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _$window_,
      _ChannelService_,
      _EditComponentService_,
      _MaskService_,
      _RightSidePanelService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      ChannelService = _ChannelService_;
      EditComponentService = _EditComponentService_;
      MaskService = _MaskService_;
      RightSidePanelService = _RightSidePanelService_;
    });

    spyOn($translate, 'instant').and.callThrough();
    spyOn(ChannelService, 'getChannel');

    ComponentEditor.open.and.returnValue($q.resolve());
    ComponentEditor.getComponentName.and.returnValue(testData.component.label);
    ChannelService.getChannel.and.returnValue(testData.channel);

    mockComponent.getId.and.returnValue(testData.component.id);
    mockComponent.getLabel.and.returnValue(testData.component.label);
    mockComponent.getLastModified.and.returnValue(testData.component.lastModified);
    mockComponent.getRenderVariant.and.returnValue(testData.component.variant);
    mockComponent.container.isInherited.and.returnValue(testData.container.isInherited);
    mockComponent.container.isDisabled.and.returnValue(testData.container.isDisabled);
    mockComponent.container.getId.and.returnValue(testData.container.id);
  });

  function editComponent() {
    EditComponentService.startEditing(mockComponent);
    $rootScope.$digest();
  }

  describe('start editing component properties', () => {
    it('clears the context label and sets the title to COMPONENT', () => {
      editComponent();

      expect(RightSidePanelService.clearContext).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('COMPONENT');
    });

    it('starts the loading state of the right side panel', () => {
      editComponent();

      expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    });

    it('opens the component editor', () => {
      editComponent();

      expect(ComponentEditor.open).toHaveBeenCalledWith('component.id', 'component.renderVariant');
    });

    it('sets the context label to COMPONENT and the title label to the component name', () => {
      editComponent();

      expect($translate.instant).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setContext).toHaveBeenCalledWith('COMPONENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('component.label');
    });

    it('stops the loading state of the right side panel', () => {
      editComponent();

      expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    });

    it('marks the service as not ready for the user', () => {
      EditComponentService.readyForUser = true;
      EditComponentService.startEditing(mockComponent);

      expect(EditComponentService.isReadyForUser()).toBe(false);
    });

    it('marks the service as ready for the user when the component editor succeeds', () => {
      EditComponentService.readyForUser = false;
      editComponent();

      expect(EditComponentService.isReadyForUser()).toBe(true);
    });

    it('stops editing when opening the component editor fails', () => {
      spyOn(EditComponentService, 'stopEditing');
      ComponentEditor.open.and.returnValue($q.reject());
      editComponent();

      expect(EditComponentService.stopEditing).toHaveBeenCalled();
    });

    it('marks the service as not ready for the user when the component editor fails', () => {
      ComponentEditor.open.and.returnValue($q.reject());
      editComponent();

      expect(EditComponentService.isReadyForUser()).toBe(false);
    });
  });

  describe('killing the editor', () => {
    it('kills the editor and stops editing', () => {
      spyOn(EditComponentService, 'stopEditing');

      EditComponentService.killEditor();

      expect(ComponentEditor.kill).toHaveBeenCalled();
      expect(EditComponentService.stopEditing).toHaveBeenCalled();
    });
  });

  it('resolves without a transition to the parent state when editing is stopped while editor is not open', (done) => {
    spyOn($state, 'go');
    EditComponentService.stopEditing()
      .then(() => {
        expect($state.go).not.toHaveBeenCalled();
        done();
      });
    $rootScope.$digest();
  });

  it('transitions to the parent state when editing is stopped while editor is open', (done) => {
    spyOn($state, 'go').and.callThrough();
    editComponent();

    EditComponentService.stopEditing()
      .then(() => {
        expect($state.go).toHaveBeenCalledWith('^');
        done();
      });
    $rootScope.$digest();
  });

  it('ignores erroneous calls and logs a warning', () => {
    spyOn($log, 'warn');
    spyOn(MaskService, 'mask');
    spyOn($window.APP_TO_CMS, 'publish');

    mockComponent = null;
    editComponent();

    expect($log.warn).toHaveBeenCalled();
    expect(MaskService.mask).not.toHaveBeenCalled();
    expect($window.APP_TO_CMS.publish).not.toHaveBeenCalled();
  });
});
