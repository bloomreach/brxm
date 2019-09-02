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

import { PageModelUrlOptions } from './api';
import { buildPageModelUrl } from './url';

const DEFAULT_OPTIONS = {
  live: {
    pageModelBaseUrl: 'http://localhost:8080/site/spa/',
  },
  preview: {
    pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa/',
  },
};

function getModelUrl(path: string, options: PageModelUrlOptions = DEFAULT_OPTIONS) {
  const request = { path };
  return buildPageModelUrl(request, options);
}

describe('buildModelUrl', () => {
  it('creates a live URL for the home page', () => {
    expect(getModelUrl('/')).toBe('http://localhost:8080/site/spa/resourceapi');
  });

  it('creates a live URL for the route "/news"', () => {
    expect(getModelUrl('/news')).toBe('http://localhost:8080/site/spa/news/resourceapi');
    expect(getModelUrl('/news/')).toBe('http://localhost:8080/site/spa/news/resourceapi');
  });

  it('creates a live URL for the page "/news/2019/foo.html"', () => {
    expect(getModelUrl('/news/2019/foo.html'))
      .toBe('http://localhost:8080/site/spa/news/2019/foo.html/resourceapi');
  });

  it('creates a preview URL for the home page', () => {
    expect(getModelUrl('/?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/resourceapi?bloomreach-preview=true');
    expect(getModelUrl('/?foo=bar&bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/resourceapi?foo=bar&bloomreach-preview=true');
  });

  it('creates a preview URL for the route "/news"', () => {
    expect(getModelUrl('/news?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/news/resourceapi?bloomreach-preview=true');

    expect(getModelUrl('/news?foo=bar&bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/news/resourceapi?foo=bar&bloomreach-preview=true');

    expect(getModelUrl('/news/?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/news/resourceapi?bloomreach-preview=true');
  });

  it('creates a preview URL for the page "/news/2019/foo.html"', () => {
    expect(getModelUrl('/news/2019/foo.html?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/news/2019/foo.html/resourceapi?bloomreach-preview=true');
  });

  it('uses the channel path relative to the SPA base URL', () => {
    const options = {
      live: {
        spaBasePath: '/site/csr-spa',
        pageModelBaseUrl: 'http://localhost:8080/site/csr-spa',
      },
      preview: {
        spaBasePath: '/site/_cmsinternal/csr-spa',
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/csr-spa',
      },
    };

    expect(getModelUrl('/site/csr-spa', options)).toBe('http://localhost:8080/site/csr-spa/resourceapi');
    expect(getModelUrl('/site/csr-spa/', options)).toBe('http://localhost:8080/site/csr-spa/resourceapi');
    expect(getModelUrl('/site/csr-spa/news', options)).toBe('http://localhost:8080/site/csr-spa/news/resourceapi');
    expect(getModelUrl('/site/_cmsinternal/csr-spa?bloomreach-preview=true', options))
      .toBe('http://localhost:8080/site/_cmsinternal/csr-spa/resourceapi?bloomreach-preview=true');
    expect(getModelUrl('/site/_cmsinternal/csr-spa/news?bloomreach-preview=true', options))
      .toBe('http://localhost:8080/site/_cmsinternal/csr-spa/news/resourceapi?bloomreach-preview=true');
  });

  it('can use an SPA base path that matches the start of routes', () => {
    const options = {
      live: {
        spaBasePath: '/site/spa-',
        pageModelBaseUrl: 'http://localhost:8080/site/spa-',
      },
      preview: {
        spaBasePath: '/site/_cmsinternal/spa-',
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa-',
      },
    };

    expect(getModelUrl('/site/spa-news', options)).toBe('http://localhost:8080/site/spa-news/resourceapi');
    expect(getModelUrl('/site/_cmsinternal/spa-news?bloomreach-preview=true', options))
      .toBe('http://localhost:8080/site/_cmsinternal/spa-news/resourceapi?bloomreach-preview=true');
  });

  it('throws an error when the request path does not start with the SPA base path', () => {
    const options = {
      live: {
        spaBasePath: '/base',
        pageModelBaseUrl: 'http://localhost:8080/site/spa',
      },
      preview: {
        spaBasePath: '/base',
        pageModelBaseUrl: 'http://localhost:8080/site/_cmsinternal/spa',
      },
    };

    expect(() => getModelUrl('/other/path', options))
      .toThrow('Request path \'/other/path\' does not start with SPA base path \'/base\'');
  });

  it('uses a custom suffix when provided', () => {
    const options = { ...DEFAULT_OPTIONS, pageModelApiSuffix: '/model' };
    expect(getModelUrl('/', options)).toBe('http://localhost:8080/site/spa/model');
    expect(getModelUrl('/news', options)).toBe('http://localhost:8080/site/spa/news/model');
    expect(getModelUrl('/?bloomreach-preview=true', options))
      .toBe('http://localhost:8080/site/_cmsinternal/spa/model?bloomreach-preview=true');
  });
});
