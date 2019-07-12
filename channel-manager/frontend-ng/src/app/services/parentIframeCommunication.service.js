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

  get parentIFrameConnection(){
    return this.cms;
  }

  set parentIFrameConnection(cms){
    this.cms = cms;
  }

  connect (callBack) {
    const methods = {
      navigate () {
      },
    };
    // TODO(mrop) Supply parentOrigin with SSO mechanism

    if (!this.parentIFrameConnection){
      const parentOrigin = '*';
      const parentConnectConfig = {parentOrigin, methods};
      connectToParent(parentConnectConfig)
        .then(parentApi => {
          this.parentIFrameConnection = parentApi;
          callBack.call();
        })
        .catch(error => console.log(error));
    }
    else{
      callBack.call();
    }

  }

  updateNavLocation (location) {
    this.connect(() => this.parentIFrameConnection.updateNavLocation(location).catch(err => console.error(err)));
  }
}

export default ParentIframeCommunicationService;
