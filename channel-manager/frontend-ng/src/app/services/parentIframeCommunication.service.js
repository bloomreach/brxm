/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
import {connectToParent} from '@bloomreach/navapp-communication';

class ParentIframeCommunicationService {

  constructor () {
    'ngInject';
  }

  get _parentIFrameConnection () {
    return this.cms;
  }

  set _parentIFrameConnection (cms) {
    this.cms = cms;
  }

  _callParent (callBack) {
    if (!this._parentIFrameConnection) {
      this._connectToParent()
        .then(parentApi => {
          this._parentIFrameConnection = parentApi;
          callBack.call();
        })
        .catch(error => console.log(error));
    }
    else {
      callBack.call();
    }
  }

  _connectToParent () {
    // TODO(mrop) Supply parentOrigin with SSO mechanism
    const parentOrigin = '*';
    const methods = {
      navigate () {
      },
    };
    const parentConnectConfig = {parentOrigin, methods};
    return connectToParent(parentConnectConfig)

  }

  updateNavLocation (location) {
    this._callParent(() => this._parentIFrameConnection.updateNavLocation(location).catch(err => console.error(err)));
  }
}

export default ParentIframeCommunicationService;
