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
  let DocumentWorkflowService;
  let FeedbackService;
  let PageService;
  let XPageMenuService;

  const allActions = [
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

    inject((
      _$q_,
      _$rootScope_,
      _DocumentWorkflowService_,
      _FeedbackService_,
      _PageService_,
      _XPageMenuService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
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
      xpage: { id: 'xpage-document-id' },
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

    it('should hide known actions', () => {
      allActions.forEach((action) => {
        expect(getAction(action).isVisible()).toBe(false);
      });
    });

    it('should show known actions', () => {
      allActions.forEach((actionId) => {
        const action = addAction(actionId);

        expect(action.isVisible()).toBe(true);
        expect(action.isEnabled()).toBe(true);
      });
    });

    it('should show disabled known actions', () => {
      allActions.forEach((actionId) => {
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
      { msg: result },
    );
  }

  function expectWorkflow(actionId, spy) {
    expectWorkflowSuccess(actionId, spy);

    spy.calls.reset();
    PageService.load.calls.reset();

    expectWorkflowFailed(actionId, spy);
  }

  describe('publish', () => {
    it('should show the "publish" action with an SVG icon', () => {
      const action = addAction('publish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconSvg()).toBe(true);
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
    it('should show the "unpublish" action with an SVG icon', () => {
      const action = addAction('unpublish');

      expect(action.isIconVisible()).toBe(true);
      expect(action.hasIconSvg()).toBe(true);
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
