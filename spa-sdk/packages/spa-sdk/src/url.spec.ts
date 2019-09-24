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

import { buildPageModelUrl } from './url';

describe('buildModelUrl', () => {
  it.each`
    path        | expected
    ${'/'}      | ${'http://localhost:8080/site/spa/resourceapi/'}
    ${'/news'}  | ${'http://localhost:8080/site/spa/resourceapi/news'}
    ${'/news/'} | ${'http://localhost:8080/site/spa/resourceapi/news/'}
    ${'/news/2019/foo.html'}                         | ${'http://localhost:8080/site/spa/resourceapi/news/2019/foo.html'}
    ${'/?bloomreach-preview=true'}                   | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/?bloomreach-preview=true'}
    ${'/?foo=bar&bloomreach-preview=true'}           | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/?foo=bar&bloomreach-preview=true'}
    ${'/news?bloomreach-preview=true'}               | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/news?bloomreach-preview=true'}
    ${'/news?foo=bar&bloomreach-preview=true'}       | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/news?foo=bar&bloomreach-preview=true'}
    ${'/news/?bloomreach-preview=true'}              | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/news/?bloomreach-preview=true'}
    ${'/news/2019/foo.html?bloomreach-preview=true'} | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/news/2019/foo.html?bloomreach-preview=true'}
  `('should create a URL based on mapping for "$path"', ({ path, expected }) => {
    const options = {
      live: {
        pageModelBaseUrl: 'http://localhost:8080/site/spa/resourceapi',
      },
      preview: {
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa/resourceapi',
      },
    };

    expect(buildPageModelUrl({ path }, options)).toBe(expected);
  });

  it.each`
    path                    | expected
    ${'/site/csr-spa'}      | ${'http://localhost:8080/site/csr-spa/resourceapi'}
    ${'/site/csr-spa/'}     | ${'http://localhost:8080/site/csr-spa/resourceapi/'}
    ${'/site/csr-spa/news'} | ${'http://localhost:8080/site/csr-spa/resourceapi/news'}
    ${'/site/_cmsinternal/csr-spa?bloomreach-preview=true'}      | ${'http://localhost:8080/site/_cmsinternal/csr-spa/resourceapi?bloomreach-preview=true'}
    ${'/site/_cmsinternal/csr-spa/news?bloomreach-preview=true'} | ${'http://localhost:8080/site/_cmsinternal/csr-spa/resourceapi/news?bloomreach-preview=true'}
  `('should use the channel path relative to the SPA base URL for "$path"', ({ path, expected }) => {
    const options = {
      live: {
        spaBasePath: '/site/csr-spa',
        pageModelBaseUrl: 'http://localhost:8080/site/csr-spa/resourceapi',
      },
      preview: {
        spaBasePath: '/site/_cmsinternal/csr-spa',
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/csr-spa/resourceapi',
      },
    };

    expect(buildPageModelUrl({ path }, options)).toBe(expected);
  });

  it.each`
    path                | expected
    ${'/site/spa-news'} | ${'http://localhost:8080/site/resourceapi/news'}
    ${'/site/_cmsinternal/spa-news?bloomreach-preview=true'} | ${'http://localhost:8080/site/_cmsinternal/resourceapi/news?bloomreach-preview=true'}
  `('should use an SPA base path that matches the start of "$path"', ({ path, expected }) => {
    const options = {
      live: {
        spaBasePath: '/site/spa-',
        pageModelBaseUrl: 'http://localhost:8080/site/resourceapi/',
      },
      preview: {
        spaBasePath: '/site/_cmsinternal/spa-',
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/resourceapi/',
      },
    };

    expect(buildPageModelUrl({ path }, options)).toBe(expected);
  });

  it.each`
    base       | path
    ${'/base'} | ${'/other/path'}
  `(
    'should throw an error because the request path "$path" does not start with the SPA base path "$base"',
    ({ base, path }) => {
      const options = {
        live: {
          spaBasePath: base,
          pageModelBaseUrl: 'http://localhost:8080/site/spa',
        },
        preview: {
          spaBasePath: base,
          pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa/resourceapi',
        },
      };

      expect(() => buildPageModelUrl({ path }, options))
        .toThrow(`Request path '${path}' does not start with SPA base path '${base}'`);
    },
  );

  it.each`
    path        | suffix     | expected
    ${'/'}      | ${'model'} | ${'http://localhost:8080/site/spa/resourceapi/model'}
    ${'/news/'} | ${'model'} | ${'http://localhost:8080/site/spa/resourceapi/news/model'}
    ${'/?bloomreach-preview=true'} | ${'something'} | ${'http://localhost:8080/site/_cmsinternal/spa/resourceapi/something?bloomreach-preview=true'}
  `('should use "$suffix" as a custom suffix for "$path"', ({ path, suffix, expected }) => {
    const options = {
      live: {
        pageModelBaseUrl: 'http://localhost:8080/site/spa/resourceapi',
        pageModelUrlSuffix: suffix,
      },
      preview: {
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa/resourceapi',
        pageModelUrlSuffix: suffix,
      },
    };

    expect(buildPageModelUrl({ path }, options)).toBe(expected);
  });
});
