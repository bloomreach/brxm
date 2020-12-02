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

import angular from 'angular';
import 'angular-mocks';

describe('DocumentWorkflowService', () => {
  let $window;
  let DocumentWorkflowService;
  let workflow;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$window_, _DocumentWorkflowService_) => {
      $window = _$window_;
      DocumentWorkflowService = _DocumentWorkflowService_;
    });

    workflow = $window.parent.Hippo.Workflow;
    spyOn(workflow, 'invoke');
  });

  it('should invoke the "copy" workflow', () => {
    DocumentWorkflowService.copy('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'document', 'copy', undefined);
  });

  it('should invoke the "move" workflow', () => {
    DocumentWorkflowService.move('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'document', 'move', undefined);
  });

  it('should invoke the "delete" workflow', () => {
    DocumentWorkflowService.delete('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'document', 'delete', undefined);
  });

  it('should invoke the "publish" workflow', () => {
    DocumentWorkflowService.publish('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'PUB', undefined);
  });

  it('should invoke the "schedule publication" workflow', () => {
    DocumentWorkflowService.schedulePublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'SCHED_PUB', undefined);
  });

  it('should invoke the "request publication" workflow', () => {
    DocumentWorkflowService.requestPublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'REQ_PUB', undefined);
  });

  it('should invoke the "request schedule publication" workflow', () => {
    DocumentWorkflowService.requestSchedulePublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'REQ_SCHED_PUB', undefined);
  });

  it('should invoke the "unpublish" workflow', () => {
    DocumentWorkflowService.unpublish('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'DEPUB', undefined);
  });

  it('should invoke the "schedule unpublication" workflow', () => {
    DocumentWorkflowService.scheduleUnpublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'SCHED_DEPUB', undefined);
  });

  it('should invoke the "request unpublication" workflow', () => {
    DocumentWorkflowService.requestUnpublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'REQ_DEPUB', undefined);
  });

  it('should invoke the "request schedule unpublication" workflow', () => {
    DocumentWorkflowService.requestScheduleUnpublication('doc1');
    expect(workflow.invoke).toHaveBeenCalledWith('doc1', 'publication', 'REQ_SCHED_DEPUB', undefined);
  });
});
