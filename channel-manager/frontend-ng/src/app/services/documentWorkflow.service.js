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

class DocumentWorkflowService {
  constructor($window) {
    'ngInject';

    this.$window = $window;
  }

  _invoke(documentId, category, action, branchId) {
    return this.$window.parent.Hippo.Workflow.invoke(documentId, category, action, branchId);
  }

  rename(documentId) {
    return this._invoke(documentId, 'document', 'rename');
  }

  copy(documentId, branchId) {
    return this._invoke(documentId, 'document', 'copy', branchId);
  }

  move(documentId) {
    return this._invoke(documentId, 'document', 'move');
  }

  delete(documentId) {
    return this._invoke(documentId, 'document', 'delete');
  }

  publish(documentId) {
    return this._invoke(documentId, 'publication', 'PUB');
  }

  schedulePublication(documentId) {
    return this._invoke(documentId, 'publication', 'SCHED_PUB');
  }

  requestPublication(documentId) {
    return this._invoke(documentId, 'publication', 'REQ_PUB');
  }

  requestSchedulePublication(documentId) {
    return this._invoke(documentId, 'publication', 'REQ_SCHED_PUB');
  }

  unpublish(documentId) {
    return this._invoke(documentId, 'publication', 'DEPUB');
  }

  scheduleUnpublication(documentId) {
    return this._invoke(documentId, 'publication', 'SCHED_DEPUB');
  }

  requestUnpublication(documentId) {
    return this._invoke(documentId, 'publication', 'REQ_DEPUB');
  }

  requestScheduleUnpublication(documentId) {
    return this._invoke(documentId, 'publication', 'REQ_SCHED_DEPUB');
  }

  cancelRequest(documentId) {
    return this._invoke(documentId, 'request', 'cancel');
  }

  acceptRequest(documentId) {
    return this._invoke(documentId, 'request', 'accept');
  }

  rejectRequest(documentId) {
    return this._invoke(documentId, 'request', 'reject');
  }

  showRequestRejected(documentId) {
    return this._invoke(documentId, 'request', 'rejected');
  }
}

export default DocumentWorkflowService;
