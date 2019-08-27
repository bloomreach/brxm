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

import { Options } from './api';
import { buildModelUrl } from './url';

const DEFAULT_OPTIONS = {
  livePrefix: 'http://localhost:8080/site/my-spa',
  previewPrefix: 'http://localhost:8080/site/_cmsinternal/my-spa'
};

function modelUrl(path: string, options: Options = DEFAULT_OPTIONS) {
  const request = { path };
  return buildModelUrl(request, options);
}

describe('buildModelUrl', () => {
  it('creates a preview URL for the home page', () => {
    expect(modelUrl('/site/_cmsinternal/my-spa')).toBe('http://localhost:8080/site/_cmsinternal/my-spa/resourceapi');
    expect(modelUrl('/site/_cmsinternal/my-spa/')).toBe('http://localhost:8080/site/_cmsinternal/my-spa/resourceapi');
  });

  it('creates a preview URL for the route "/news"', () => {
    expect(modelUrl('/site/_cmsinternal/my-spa/news')).toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/resourceapi');
    expect(modelUrl('/site/_cmsinternal/my-spa/news/')).toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/resourceapi');
  });

  it('creates a preview URL for the detail page "/news/2019/foo.html"', () => {
    expect(modelUrl('/site/_cmsinternal/my-spa/news/2019/foo.html'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/2019/foo.html/resourceapi');
  });

  it('creates a preview URL for the home page when the query parameter "bloomreach-preview" is true', () => {
    expect(modelUrl('?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/resourceapi?bloomreach-preview=true');

    expect(modelUrl('?foo=bar&bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/resourceapi?foo=bar&bloomreach-preview=true');

    expect(modelUrl('/?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/resourceapi?bloomreach-preview=true');
  });

  it('creates a preview URL for the route "/news" when the query parameter "bloomreach-preview" is true', () => {
    expect(modelUrl('/news?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/resourceapi?bloomreach-preview=true');

    expect(modelUrl('/news?foo=bar&bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/resourceapi?foo=bar&bloomreach-preview=true');

    expect(modelUrl('/news/?bloomreach-preview=true'))
      .toBe('http://localhost:8080/site/_cmsinternal/my-spa/news/resourceapi?bloomreach-preview=true');
  });

  it('creates a live URL for the home page', () => {
    expect(modelUrl('')).toBe('http://localhost:8080/site/my-spa/resourceapi');
    expect(modelUrl('/')).toBe('http://localhost:8080/site/my-spa/resourceapi');
    expect(modelUrl('/site/my-spa')).toBe('http://localhost:8080/site/my-spa/resourceapi');
    expect(modelUrl('/site/my-spa/')).toBe('http://localhost:8080/site/my-spa/resourceapi');
  });

  it('creates a live URL for the route "/news"', () => {
    expect(modelUrl('/news')).toBe('http://localhost:8080/site/my-spa/news/resourceapi');
    expect(modelUrl('/news/')).toBe('http://localhost:8080/site/my-spa/news/resourceapi');
    expect(modelUrl('/site/my-spa/news')).toBe('http://localhost:8080/site/my-spa/news/resourceapi');
    expect(modelUrl('/site/my-spa/news/')).toBe('http://localhost:8080/site/my-spa/news/resourceapi');
  });

  it('creates a live URL for the detail page "/news/2019/foo.html"', () => {
    expect(modelUrl('/site/my-spa/news/2019/foo.html'))
      .toBe('http://localhost:8080/site/my-spa/news/2019/foo.html/resourceapi');
  });

  it('uses a custom suffix when provided', () => {
    const options = Object.assign({}, DEFAULT_OPTIONS, { apiSuffix: '/model' });
    expect(modelUrl('/', options)).toBe('http://localhost:8080/site/my-spa/model');
    expect(modelUrl('/site/my-spa', options)).toBe('http://localhost:8080/site/my-spa/model');
    expect(modelUrl('/site/_cmsinternal/my-spa', options)).toBe('http://localhost:8080/site/_cmsinternal/my-spa/model');
    expect(modelUrl('/?bloomreach-preview=true', options)).toBe('http://localhost:8080/site/_cmsinternal/my-spa/model?bloomreach-preview=true');
  });
});
