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
import { connectToParent } from '@bloomreach/navapp-communication';

class ParentIframeCommunicationService {
  constructor(ChannelService) {
    'ngInject';

    this.ChannelService = ChannelService;

    const parentOrigin = window.location.origin;
    const methods = {
      // eslint-disable-next-line no-unused-vars
      navigate: (location, flags) => {
        this.updateNavLocation(this._getLocation(flags));
      },
    };
    this.apiPromise = connectToParent({ parentOrigin, methods });
  }

  updateNavLocation(location) {
    this.apiPromise.then((api) => {
      api.updateNavLocation(location)
        .catch(error => console.error(error));
    }).catch(error => console.error('Connection to parent failed', error));
  }

  _getLocation(flags) {
    if (this.ChannelService.hasChannel() && !(flags && flags.forceRefresh)) {
      return {
        breadcrumbLabel: this.ChannelService.getName(),
        path: `channelmanager/${this.ChannelService.getId()}`,
      };
    }
    return { path: 'channelmanager' };
  }
}

export default ParentIframeCommunicationService;
