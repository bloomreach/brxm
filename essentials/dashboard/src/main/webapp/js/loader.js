/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

/**
 * loader.js loads the dynamically contributed parts (by Essentials plugins) of the dashboard
 * AngularJS application by retrieving a list of files from the back-end (PluginResource#getModules)
 * and loading them before bootstrapping the AngularJS application. The back-end URL is provided
 * through the 'data-modules' attribute of the <script> tag which loads this script (loader.js).
 */
(function () {
  'use strict';

  function getScriptsUrl() {
    var scriptTags = document.getElementsByTagName('script');
    for (var i = scriptTags.length - 1; i > 0; i--) {
      var scriptPath = scriptTags[i].src;
      var filename = scriptPath.substring(scriptPath.lastIndexOf('/') + 1);

      if (filename == 'loader.js' || filename == 'loader.min.js') {
        return scriptTags[i].getAttribute('data-modules');
      }
    }
    console.warn('No \'data-modules\' URL specified for retrieving dynamic scripts.');
    return undefined;
  }

  function loadScripts(url) {
    if (url) {
      $.ajax({
        url: url,
        dataType: 'json'
      }).done(function (applicationData) {
        $.each(applicationData.files, function (name, file) {
          $('head').append('<script src=\"' + file + '\" />');
        });
        $('head').append('<script>angular.bootstrap(document.getElementById(\'container\'), [\'' + applicationData.name + '\']);</' + 'script>');
      }).fail(function (jqXHR, textStatus, errorThrown) {
        errorThrown.message = '[Loader error s(' + textStatus + ')]: ' + errorThrown;
      });
    }
  }

  loadScripts(getScriptsUrl());
})();