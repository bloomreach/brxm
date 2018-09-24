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

describe('EditComponentMainCtrl', () => {
  let $log;
  let $q;
  let $scope;
  let $translate;
  let ChannelService;
  let CmsService;
  let ComponentEditor;
  let EditComponentService;
  let FeedbackService;
  let HippoIframeService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.rightSidePanel.editComponent');

    inject((
      $controller,
      $rootScope,
      _$log_,
      _$q_,
      _$translate_,
      _EditComponentService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $translate = _$translate_;
      EditComponentService = _EditComponentService_;

      ChannelService = jasmine.createSpyObj('ChannelService', ['recordOwnChange']);
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
        'save',
      ]);
      FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

      $scope = $rootScope.$new();
      $ctrl = $controller('editComponentMainCtrl as $ctrl', {
        $scope,
        ChannelService,
        CmsService,
        ComponentEditor,
        EditComponentService,
        FeedbackService,
        HippoIframeService,
      });

      $scope.$digest();
    });
  });

  it('allows save when the component editor is dirty', () => {
    ComponentEditor.dirty = false;
    expect($ctrl.isSaveAllowed()).toBe(false);

    ComponentEditor.dirty = true;
    expect($ctrl.isSaveAllowed()).toBe(true);
  });

  describe('save component', () => {
    it('should show a toaster on error', (done) => {
      const parameterMap = {};
      ComponentEditor.save.and.returnValue($q.reject({
        data: {
          error: 'ITEM_ALREADY_LOCKED',
          parameterMap,
        },
      }));

      spyOn($translate, 'instant');
      $translate.instant.and.returnValue('translated');

      $ctrl.save()
        .then(() => {
          expect($translate.instant).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED', parameterMap);
          expect(FeedbackService.showError).toHaveBeenCalledWith('translated');
          expect(ComponentEditor.save).toHaveBeenCalled();
          done();
        });
      $scope.$digest();
    });

    it('should report usage statistics', (done) => {
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
    it('does discard changes when confirmed', () => {
      ComponentEditor.confirmDiscardChanges.and.returnValue($q.resolve());

      $ctrl.discard();
      $scope.$digest();

      expect(ComponentEditor.discardChanges).toHaveBeenCalled();
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
      });

      it('fails silently when save or discard changes is canceled', (done) => {
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ComponentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
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
        ComponentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
        ComponentEditor.component = { id: 'componentId' };
      });

      it('closes the component editor ', (done) => {
        $ctrl.uiCanExit().then(() => {
          expect(ComponentEditor.close).toHaveBeenCalled();
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
        spyOn(EditComponentService, 'stopEditing');

        $ctrl.deleteComponent();
        $scope.$digest();
      });

      it('deletes the component if the action is confirmed', () => {
        expect(ComponentEditor.deleteComponent).toHaveBeenCalled();
      });

      it('records a change by the current user', () => {
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
      });

      it('reloads the page', () => {
        expect(HippoIframeService.reload).toHaveBeenCalled();
      });

      it('closes the component editor', () => {
        expect(EditComponentService.stopEditing).toHaveBeenCalled();
      });
    });
  });
});
