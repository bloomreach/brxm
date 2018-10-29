/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const uiProperties = {
  baseUrl: 'https://cms.example.com',
  extension: {
    config: 'testConfig',
  },
  locale: 'en',
  timeZone: 'Europe/Amsterdam',
  user: 'admin',
  version: '13.0.0',
};

const parent = {
  getProperties: () => Promise.resolve(uiProperties),
};

const penpal = {
  Promise,
  connectToParent: jest.fn(() => ({
    promise: Promise.resolve(parent),
  })),
  ERR_CONNECTION_DESTROYED: 'ConnectionDestroyed',
  ERR_CONNECTION_TIMEOUT: 'ConnectionTimeout',
  ERR_NOT_IN_IFRAME: 'NotInIframe',
};

export default penpal;
