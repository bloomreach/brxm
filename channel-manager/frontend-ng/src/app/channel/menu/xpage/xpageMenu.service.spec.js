/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('XPageMenuService', () => {
  let $q;
  let $rootScope;
  let $state;
  let DocumentWorkflowService;
  let FeedbackService;
  let PageService;
  let PageToolsService;
  let XPageMenuService;

  const allWorkflowActions = [
    'publish',
    'schedule-publish',
    'request-publish',
    'request-schedule-publish',
    'unpublish',
    'schedule-unpublish',
    'request-unpublish',
    'request-schedule-unpublish',
  ];

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    PageToolsService = jasmine.createSpyObj('PageToolsService', [
      'hasExtensions',
      'showPageTools',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('PageToolsService', PageToolsService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _DocumentWorkflowService_,
      _FeedbackService_,
      _PageService_,
      _XPageMenuService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      DocumentWorkflowService = _DocumentWorkflowService_;
      FeedbackService = _FeedbackService_;
      PageService = _PageService_;
      XPageMenuService = _XPageMenuService_;
    });

    spyOn(DocumentWorkflowService, 'publish').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'schedulePublication').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'requestPublication').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'requestSchedulePublication').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'unpublish').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'scheduleUnpublication').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'requestUnpublication').and.returnValue($q.resolve());
    spyOn(DocumentWorkflowService, 'requestScheduleUnpublication').and.returnValue($q.resolve());

    spyOn(PageService, 'load');

    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showNotification');
  });

  function getAction(name) {
    return XPageMenuService.menu.items.find(item => item.name === name);
  }

  function addAction(name, enabled = true) {
    if (!PageService.actions) {
      PageService.actions = {
        xpage: {
          items: {},
        },
      };
    }
    PageService.actions.xpage.items[name] = {
      enabled,
    };

    return getAction(name);
  }

  beforeEach(() => {
    PageService.actions = null;
    PageService.states = {
      xpage: {
        id: 'xpage-document-id',
        name: 'xpage-document-name',
      },
    };
  });

  describe('xpage menu', () => {
    it('should hide the menu button', () => {
      expect(XPageMenuService.menu.isVisible()).toBe(false);
    });

    it('should show the menu button', () => {
      PageService.actions = {
        xpage: {},
      };

      expect(XPageMenuService.menu.isVisible()).toBe(true);
    });

    it('queries the PageToolsService for extensions to check if "tools" action is visible', () => {
      getAction('tools').isVisible();

      expect(PageToolsService.hasExtensions).toHaveBeenCalled();
    });

    it('should show the page tools when clicked', () => {
      getAction('tools').onClick();

      expect(PageToolsService.showPageTools).toHaveBeenCalled();
    });

    it('should open the content editor', () => {
      spyOn($state, 'go');
      getAction('content').onClick();

      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.edit-page', {
        documentId: 'xpage-document-id',
        showVersionsInfo: false,
      });
    });

    it('should open the versions panel', () => {
      spyOn($state, 'go');
      getAction('versions').onClick();

      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.edit-page', {
        documentId: 'xpage-document-id',
        showVersionsInfo: true,
      });
    });

    it('should hide workflow actions', () => {
      allWorkflowActions.forEach((action) => {
        expect(getAction(action).isVisible()).toBe(false);
      });
    });

    it('should show workflow actions', () => {
      allWorkflowActions.forEach((actionId) => {
        const action = addAction(actionId);

        expect(action.isVisible()).toBe(true);
        expect(action.isEnabled()).toBe(true);
      });
    });

    it('should show disabled workflow actions', () => {
      allWorkflowActions.forEach((actionId) => {
        const action = addAction(actionId, false);

        expect(action.isVisible()).toBe(true);
        expect(action.isEnabled()).toBe(false);
      });
    });
  });

  function expectWorkflowSuccess(actionId, spy) {
    const result = `Workflow[${actionId}] resolved`;
    spy.and.returnValue($q.resolve(result));
    const action = addAction(actionId);

    action.onClick();
    $rootScope.$digest();

    expect(spy).toHaveBeenCalledWith('xpage-document-id');
    expect(PageService.load).toHaveBeenCalled();
  }

  function expectWorkflowFailed(actionId, spy) {
    const result = `Workflow[${actionId}] rejected`;
    spy.and.returnValue($q.reject(result));
    const action = addAction(actionId);

    action.onClick();
    $rootScope.$digest();

    expect(spy).toHaveBeenCalledWith('xpage-document-id');
    expect(PageService.load).toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith(
      `${action.translationKey}_ERROR`,
      {
        msg: result,
        documentName: 'xpage-document-name',
      },
    );
  }

  function expectWorkflow(actionId, spy) {
    expectWorkflowSuccess(actionId, spy);

    spy.calls.reset();
    PageService.load.calls.reset();

    expectWorkflowFailed(actionId, spy);
  }

  describe('publish', () => {
    it('should show the "publish" action with an icon', () => {
      const action = addAction('publish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconName()).toBe(true);
    });

    it('should call DocumentWorkflowService.publish()', () => {
      expectWorkflow('publish', DocumentWorkflowService.publish);
    });
  });

  describe('schedule-publish', () => {
    it('should call DocumentWorkflowService.schedulePublication()', () => {
      expectWorkflow('schedule-publish', DocumentWorkflowService.schedulePublication);
    });
  });

  describe('request-publish', () => {
    it('should show the "request-publish" action with an icon', () => {
      const action = addAction('request-publish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconName()).toBe(true);
    });

    it('should call DocumentWorkflowService.requestPublication()', () => {
      expectWorkflow('request-publish', DocumentWorkflowService.requestPublication);
    });
  });

  describe('request-schedule-publish', () => {
    it('should call DocumentWorkflowService.requestSchedulePublication()', () => {
      expectWorkflow('request-schedule-publish', DocumentWorkflowService.requestSchedulePublication);
    });
  });

  describe('unpublish', () => {
    it('should show the "unpublish" action with an icon', () => {
      const action = addAction('unpublish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconName()).toBe(true);
    });

    it('should call DocumentWorkflowService.unpublish()', () => {
      expectWorkflow('unpublish', DocumentWorkflowService.unpublish);
    });
  });

  describe('schedule-unpublish', () => {
    it('should call DocumentWorkflowService.scheduleUnpublication()', () => {
      expectWorkflow('schedule-unpublish', DocumentWorkflowService.scheduleUnpublication);
    });
  });

  describe('request-unpublish', () => {
    it('should show the "request-unpublish" action with an icon', () => {
      const action = addAction('request-unpublish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconName()).toBe(true);
    });

    it('should call DocumentWorkflowService.requestUnpublication()', () => {
      expectWorkflow('request-unpublish', DocumentWorkflowService.requestUnpublication);
    });
  });

  describe('request-schedule-unpublish', () => {
    it('should call DocumentWorkflowService.requestScheduleUnpublication()', () => {
      expectWorkflow('request-schedule-unpublish', DocumentWorkflowService.requestScheduleUnpublication);
    });
  });

  describe('workflow cancelled by the user', () => {
    it('should not show a toast when a workflow action is rejected with a "cancelled" flag', () => {
      const action = addAction('publish');

      DocumentWorkflowService.publish.and.returnValue($q.reject('{ "cancelled": true }'));

      action.onClick();
      $rootScope.$digest();

      expect(FeedbackService.showError).not.toHaveBeenCalled();
    });
  });
});
