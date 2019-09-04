/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import { initialize } from './index';

const options = {
  live: {
    pageModelBaseUrl: 'http://localhost:8080/site/my-spa',
  },
  preview: {
    pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/my-spa',
  },
};

describe('initialize', () => {
  it('fetches the page model', async () => {
    const request = { path: '/', headers: { 'Cookie': 'JSESSIONID=1234' } };
    const httpClient = jest.fn(() => Promise.resolve({ page: { type: 'COMPONENT' } }));

    await initialize({ httpClient, request, options });

    expect(httpClient).toHaveBeenCalledWith({
      method: 'get',
      url: 'http://localhost:8080/site/my-spa/resourceapi',
      headers: { 'Cookie': 'JSESSIONID=1234' },
    });
  });

  it('rejects when fetching the page model fails', () => {
    const request = { path: '/' };
    const error = Error('Failed to fetch page model data');
    const httpClient = () => { throw error };

    expect.assertions(1);
    expect(initialize({ httpClient, request, options })).rejects.toBe(error)
  });
});
