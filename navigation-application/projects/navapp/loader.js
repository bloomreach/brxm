/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

(function NavappLoader() {
  const navAppSettings = window.NavAppSettings;
  let navAppResourceLocation = '.';

  if (
    navAppSettings &&
    navAppSettings.appSettings &&
    navAppSettings.appSettings.navAppResourceLocation
  ) {
    navAppResourceLocation = navAppSettings.appSettings.navAppResourceLocation;
  }

  const baseTag = document.createElement('base');
  baseTag.href = navAppResourceLocation;
  document.head.appendChild(baseTag);

  function getQueryParameters(scriptName) {
    const scripts = Array.from(document.getElementsByTagName('script'));
    const script = scripts.find(script =>
      script.src.includes(`/${scriptName}`),
    );
    return new URL(script.src).searchParams;
  }

  const queryParameters = getQueryParameters('loader.js');
  const antiCache = queryParameters.get('antiCache');

  let fileList = `${navAppResourceLocation}/filelist.json`;
  if (antiCache) {
    fileList += `?antiCache=${antiCache}`;
  }

  fetch(fileList)
    .then(response => response.json())
    .then(files => {
      const urls = Object.values(files);

      urls
        .filter(url => url.endsWith('.css'))
        .forEach(url => {
          const linkTag = document.createElement('link');
          linkTag.rel = 'stylesheet';
          linkTag.href = `${navAppResourceLocation}/${url}`;
          document.head.appendChild(linkTag);
        });

      urls
        .filter(url => url.endsWith('.js'))
        .forEach(url => {
          const scriptTag = document.createElement('script');
          scriptTag.src = `${navAppResourceLocation}/${url}`;
          document.body.appendChild(scriptTag);
        });
    });
})();
