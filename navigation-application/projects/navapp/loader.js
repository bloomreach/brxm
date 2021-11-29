/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
  function getQueryParameters(scriptName) {
    const scripts = Array.from(document.getElementsByTagName('script'));
    const script = scripts.find(script =>
      script.src.includes(`/${scriptName}`),
    );
    return new URL(script.src).searchParams;
  }

  function doesElementExist(selector) {
    return !!document.querySelector(selector);
  }

  function addStyleSheet(url) {
    if (!url) {
      return;
    }

    const linkTag = document.createElement('link');
    linkTag.rel = 'stylesheet';
    linkTag.href = url;
    document.head.appendChild(linkTag);
  }

  function addScript(url) {
    if (!url) {
      return;
    }

    const scriptTag = document.createElement('script');
    scriptTag.src = url;
    scriptTag.defer = true;
    document.body.appendChild(scriptTag);
  }

  const navAppSettings = window.NavAppSettings || {};
  let navAppResourceLocation = navAppSettings.appSettings?.navAppResourceLocation || '.';

  if (!navAppResourceLocation.endsWith('/')) {
    navAppResourceLocation += '/';
  }

  const baseTag = document.createElement('base');
  baseTag.href = navAppResourceLocation;
  document.head.appendChild(baseTag);

  const queryParameters = getQueryParameters('loader.js');
  const antiCache = queryParameters.get('antiCache');

  let fileList = 'filelist.json';
  if (antiCache) {
    fileList += `?antiCache=${antiCache}`;
  }

  fetch(fileList)
    .then(response => response.json())
    .then(files => {
      // The order matters when loading these scripts so we have to force certain
      // scripts to be loaded before others. Be aware this might need to change
      // whenever we are changing angular versions! Specifically zone.js needs to
      // be loaded before runtime and main.
      // The default order in angular *11* seems to be:
      // runtime
      // polyfills
      // scripts
      // vendor
      // main

      addScript(files['runtime.js']);
      addScript(files['polyfills.js']);
      addScript(files['scripts.js']);
      addScript(files['vendor.js']);
      addScript(files['main.js']);

      // Add all the rest of the files to the DOM
      const urls = Object.values(files);
      urls
        .filter(url => url.endsWith('.css'))
        .filter(url => !doesElementExist(`link[href="${url}"]`))
        .forEach(url => addStyleSheet(url));

      urls
        .filter(url => url.endsWith('.js'))
        .filter(url => !doesElementExist(`script[src="${url}"]`))
        .forEach(url => addScript(url));
    });
})();
