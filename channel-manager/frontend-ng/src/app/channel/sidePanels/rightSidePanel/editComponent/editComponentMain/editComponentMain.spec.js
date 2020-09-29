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

describe('EditComponentMainCtrl', () => {
  let $log;
  let $q;
  let $rootScope;
  let $scope;
  let ChannelService;
  let CmsService;
  let ComponentEditor;
  let EditComponentService;
  let FeedbackService;
  let HippoIframeService;

  let $ctrl;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((
      $componentController,
      _$rootScope_,
      _$log_,
      _$q_,
      _EditComponentService_,
    ) => {
      $rootScope = _$rootScope_;
      $log = _$log_;
      $q = _$q_;
      EditComponentService = _EditComponentService_;

      ChannelService = jasmine.createSpyObj('ChannelService', ['checkChanges']);
      CmsService = jasmine.createSpyObj('CmsService', [
        'publish',
        'reportUsageStatistic',
      ]);
      ComponentEditor = jasmine.createSpyObj('ComponentEditor', [
        'close',
        'confirmDeleteComponent',
        'confirmDiscardChanges',
        'confirmSaveOrDiscardChanges',
        'deleteComponent',
        'discardChanges',
        'getComponentId',
        'getComponentName',
        'getPropertyGroups',
        'isKilled',
        'isReadOnly',
        'reopen',
        'save',
        'updatePreview',
      ]);
      FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);
      EditComponentService = jasmine.createSpyObj('EditComponentService', ['isReadyForUser', 'killEditor']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

      $scope = $rootScope.$new();
      $ctrl = $componentController('editComponentMain', {
        $scope,
        ChannelService,
        CmsService,
        ComponentEditor,
        EditComponentService,
        FeedbackService,
        HippoIframeService,
      });

      form = jasmine.createSpyObj('form', ['$setPristine']);
      $ctrl.form = form;

      $scope.$digest();
    });
  });

  describe('handling of component:moved event', () => {
    it('redraws the preview of the component being edited', () => {
      $ctrl.$onInit();
      $rootScope.$emit('component:moved');

      expect(ComponentEditor.updatePreview).toHaveBeenCalled();
    });
  });

  describe('handling of page:change event', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('should redraw the preview of the edited component on an initial render', () => {
      $rootScope.$emit('page:change', { initial: true });

      expect(ComponentEditor.updatePreview).toHaveBeenCalled();
    });

    it('should not redraw the preview of the edited component on not an initial render', () => {
      $rootScope.$emit('page:change');
      $rootScope.$emit('page:change', { initial: false });

      expect(ComponentEditor.updatePreview).not.toHaveBeenCalled();
    });

    it('should stop reacting on page:change events after destruction', () => {
      $ctrl.$onDestroy();
      $rootScope.$emit('page:change');

      expect(ComponentEditor.updatePreview).not.toHaveBeenCalled();
    });
  });

  describe('handling of select-document clicks in the overlay', () => {
    const eventData = {
      containerItemId: '1',
      parameterName: 'parameterName',
    };

    beforeEach(() => {
      spyOn($scope, '$broadcast');
      ComponentEditor.getComponentId.and.returnValue('1');
    });

    it('should broadcast an event when a document is selected for the currently edited component', () => {
      $ctrl.$onInit();
      $rootScope.$emit('iframe:document:select', eventData);

      expect($scope.$broadcast).toHaveBeenCalledWith('edit-component:select-document', 'parameterName');
    });

    it('should not broadcast an event when a document is selected for a component that is not being edited', () => {
      ComponentEditor.getComponentId.and.returnValue('2');

      $ctrl.$onInit();
      $rootScope.$emit('iframe:document:select', eventData);

      expect($scope.$broadcast).not.toHaveBeenCalled();
    });

    it('should prevent default handler when a document is selected for the currently edited component', () => {
      const listener = jasmine.createSpy();
      $rootScope.$on('iframe:document:select', listener);
      $ctrl.$onInit();
      $rootScope.$emit('iframe:document:select', eventData);

      const { args: [event] } = listener.calls.mostRecent();

      expect(event.defaultPrevented).toBe(true);
    });

    it('should unsubscribe from the document:select event on destroy', () => {
      $ctrl.$onInit();
      $ctrl.$onDestroy();
      $rootScope.$emit('iframe:document:select', eventData);

      expect($scope.$broadcast).not.toHaveBeenCalled();
    });
  });

  it('gets the property groups', () => {
    const propertyGroups = [];
    ComponentEditor.getPropertyGroups.and.returnValue(propertyGroups);
    expect($ctrl.getPropertyGroups()).toBe(propertyGroups);
  });

  describe('hasNoProperties', () => {
    it('returns true when there are no properties', () => {
      ComponentEditor.getPropertyGroups.and.returnValue([]);
      expect($ctrl.hasNoProperties()).toBe(true);
    });

    it('returns false when there are properties', () => {
      ComponentEditor.getPropertyGroups.and.returnValue([{}]);
      expect($ctrl.hasNoProperties()).toBe(false);
    });

    it('returns false when no properties have been loaded yet', () => {
      ComponentEditor.getPropertyGroups.and.returnValue(undefined);
      expect($ctrl.hasNoProperties()).toBe(false);
    });
  });

  it('gets the read-only state', () => {
    ComponentEditor.isReadOnly.and.returnValue(true);
    expect($ctrl.isReadOnly()).toBe(true);

    ComponentEditor.isReadOnly.and.returnValue(false);
    expect($ctrl.isReadOnly()).toBe(false);
  });

  describe('isDeleteDisabled', () => {
    it('is disabled when the component is read-only', () => {
      ComponentEditor.isReadOnly.and.returnValue(true);
      expect($ctrl.isDeleteDisabled()).toBe(true);
    });
    it('is disabled when the editor is not ready for the user yet', () => {
      ComponentEditor.isReadOnly.and.returnValue(false);
      EditComponentService.isReadyForUser.and.returnValue(false);
      expect($ctrl.isDeleteDisabled()).toBe(true);
    });
  });

  describe('isSaveAllowed', () => {
    it('returns falsy when the form does not exist yet', () => {
      delete $ctrl.form;
      expect($ctrl.isSaveAllowed()).toBeFalsy();
    });

    it('returns false when the form is neither dirty nor valid', () => {
      form.$dirty = false;
      form.$valid = false;
      expect($ctrl.isSaveAllowed()).toBe(false);
    });

    it('returns false when the form is dirty but not valid', () => {
      form.$dirty = true;
      form.$valid = false;
      expect($ctrl.isSaveAllowed()).toBe(false);
    });

    it('returns false when the form is not dirty and valid', () => {
      form.$dirty = false;
      form.$valid = true;
      expect($ctrl.isSaveAllowed()).toBe(false);
    });

    it('returns true when the form is dirty and valid', () => {
      form.$dirty = true;
      form.$valid = true;
      expect($ctrl.isSaveAllowed()).toBe(true);
    });

    it('returns false when the editor is in read only mode', () => {
      form.$dirty = true;
      form.$valid = true;
      spyOn($ctrl, 'isReadOnly');
      $ctrl.isReadOnly.and.returnValue(true);

      expect($ctrl.isSaveAllowed()).toBe(false);
    });
  });

  describe('isDiscardAllowed', () => {
    it('returns falsy when the form does not exist yet', () => {
      delete $ctrl.form;
      expect($ctrl.isDiscardAllowed()).toBeFalsy();
    });

    it('returns false when the form is pristine', () => {
      form.$dirty = false;
      expect($ctrl.isDiscardAllowed()).toBe(false);
    });

    it('returns true when the form is dirty', () => {
      form.$dirty = true;
      expect($ctrl.isDiscardAllowed()).toBe(true);
    });

    it('returns false when the editor is in read only mode', () => {
      form.$dirty = true;
      spyOn($ctrl, 'isReadOnly');
      $ctrl.isReadOnly.and.returnValue(true);

      expect($ctrl.isDiscardAllowed()).toBe(false);
    });
  });

  describe('save component', () => {
    it('fails with a message when another user locked the component\'s container', (done) => {
      const parameterMap = {};
      ComponentEditor.save.and.returnValue($q.reject({
        data: {
          error: 'ITEM_ALREADY_LOCKED',
          parameterMap,
        },
      }));
      ComponentEditor.reopen.and.returnValue($q.resolve());

      $ctrl.save()
        .then(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith(
            'ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED', parameterMap,
          );
          expect(HippoIframeService.reload).toHaveBeenCalled();
          expect(ComponentEditor.save).toHaveBeenCalled();
          done();
        });
      $scope.$digest();
    });

    it('fails with a message when another user deleted the component', (done) => {
      ComponentEditor.save.and.returnValue($q.reject({
        message: 'javax.jcr.ItemNotFoundException: some-uuid',
        data: {
          error: null,
        },
      }));

      $ctrl.save()
        .then(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT', undefined);
          expect(HippoIframeService.reload).toHaveBeenCalled();
          expect(ComponentEditor.save).toHaveBeenCalled();
          expect(EditComponentService.killEditor).toHaveBeenCalled();
          done();
        });
      $scope.$digest();
    });

    it('makes the form pristine when saving changes succeeds', (done) => {
      ComponentEditor.save.and.returnValue($q.resolve());
      $ctrl.save().then(() => {
        expect(form.$setPristine).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('keeps the form dirty when saving changes fails', (done) => {
      ComponentEditor.save.and.returnValue($q.reject());

      $ctrl.save().catch(() => {
        expect(form.$setPristine).not.toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('should report usage statistics when the save succeeds', (done) => {
      ComponentEditor.save.and.returnValue($q.resolve(''));
      $ctrl.save()
        .then(() => {
          expect(ComponentEditor.save).toHaveBeenCalled();
          expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsSaveComponent');

          done();
        });
      $scope.$digest();
    });
  });

  describe('discard component changes', () => {
    it('does discard changes when confirmed and makes the form pristine again', () => {
      ComponentEditor.confirmDiscardChanges.and.returnValue($q.resolve());

      $ctrl.discard();
      $scope.$digest();

      expect(ComponentEditor.discardChanges).toHaveBeenCalled();
      expect(form.$setPristine).toHaveBeenCalled();
    });

    it('does not discard changes when not confirmed', () => {
      ComponentEditor.confirmDiscardChanges.and.returnValue($q.reject());

      $ctrl.discard();
      $scope.$digest();

      expect(ComponentEditor.discardChanges).not.toHaveBeenCalled();
    });
  });

  describe('ui-router state exit', () => {
    describe('when save or discard changes is rejected', () => {
      beforeEach(() => {
        spyOn($log, 'error');
        form.$dirty = true;
        form.$valid = true;
      });

      it('fails silently when save or discard changes is canceled', (done) => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ComponentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith(true);
          expect($log.error).not.toHaveBeenCalled();
          expect(ComponentEditor.close).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('logs an error when save or discard changes throws an error', (done) => {
        const error = new Error('test-error');
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject(error));

        $ctrl.uiCanExit().catch(() => {
          expect($log.error).toHaveBeenCalledWith('An error occurred while closing the ComponentEditor ->', error);
          expect(ComponentEditor.close).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });
    });

    describe('when save or discard changes is resolved', () => {
      beforeEach(() => {
        ComponentEditor.component = { id: 'componentId' };
        spyOn($ctrl, 'save');
      });

      it('calls save and closes the component editor when save is resolved', (done) => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('SAVE'));

        $ctrl.uiCanExit().then(() => {
          expect($ctrl.save).not.toHaveBeenCalled();
          expect(ComponentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('does not save and closes the component editor when discard changes is resolved', (done) => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('DISCARD'));

        $ctrl.uiCanExit().then(() => {
          expect($ctrl.save).not.toHaveBeenCalled();
          expect(ComponentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });
    });

    describe('when the editor is killed', () => {
      it('checks for the kill status of the component editor', () => {
        ComponentEditor.isKilled.and.returnValue(true);
        form.$dirty = true;

        expect($ctrl.uiCanExit()).toBe(true);
        expect(ComponentEditor.confirmSaveOrDiscardChanges).not.toHaveBeenCalled();
      });
    });

    describe('when the editor is in the read only mode', () => {
      it('checks for the read only mode of the component editor', () => {
        spyOn($ctrl, 'isReadOnly');
        $ctrl.isReadOnly.and.returnValue(true);
        ComponentEditor.isKilled.and.returnValue(false);
        form.$dirty = true;

        expect($ctrl.uiCanExit()).toBe(true);
        expect(ComponentEditor.confirmSaveOrDiscardChanges).not.toHaveBeenCalled();
      });
    });

    describe('when the changes are invalid', () => {
      it('uses the validation state to confirm or discard changes', (done) => {
        form.$dirty = true;
        form.$valid = false;
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ComponentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith(false);
          done();
        });
        $scope.$digest();
      });
    });
  });

  describe('delete component', () => {
    it('shows a confirm delete dialog', () => {
      ComponentEditor.confirmDeleteComponent.and.returnValue($q.resolve());

      $ctrl.deleteComponent();
      $scope.$digest();

      expect(ComponentEditor.confirmDeleteComponent).toHaveBeenCalled();
    });

    it('does not delete the component if the action is cancelled', () => {
      ComponentEditor.confirmDeleteComponent.and.returnValue($q.reject());

      $ctrl.deleteComponent();
      $scope.$digest();

      expect(ComponentEditor.deleteComponent).not.toHaveBeenCalled();
    });

    describe('when delete succeeds', () => {
      beforeEach(() => {
        ComponentEditor.deleteComponent.and.returnValue($q.resolve());
        ComponentEditor.confirmDeleteComponent.and.returnValue($q.resolve());

        $ctrl.deleteComponent();
        $scope.$digest();
      });

      it('deletes the component if the action is confirmed', () => {
        expect(ComponentEditor.deleteComponent).toHaveBeenCalled();
      });

      it('records a change by the current user', () => {
        expect(ChannelService.checkChanges).toHaveBeenCalled();
      });

      it('reloads the page', () => {
        expect(HippoIframeService.reload).toHaveBeenCalled();
      });

      it('kills the component editor', () => {
        expect(EditComponentService.killEditor).toHaveBeenCalled();
      });
    });

    describe('when the delete fails it reloads the page and', () => {
      const resultParameters = {
        component: 'componentName',
      };

      beforeEach(() => {
        ComponentEditor.confirmDeleteComponent.and.returnValue($q.resolve());
        ComponentEditor.getComponentName.and.returnValue('componentName');
      });

      it('shows a message if the component was locked by another user and reopens the editor', () => {
        ComponentEditor.deleteComponent.and.returnValue($q.reject({
          error: 'ITEM_ALREADY_LOCKED',
          parameterMap: {},
        }));

        $ctrl.deleteComponent();
        $scope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith(
          'ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED', resultParameters,
        );
        expect(HippoIframeService.reload).toHaveBeenCalled();
      });

      it('shows a default message if a general delete error occurs', () => {
        ComponentEditor.deleteComponent.and.returnValue($q.reject({
          parameterMap: {},
        }));

        $ctrl.deleteComponent();
        $scope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT', resultParameters);
        expect(HippoIframeService.reload).toHaveBeenCalled();
      });
    });
  });
});
