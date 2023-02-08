/*!
 * Copyright 2019-2023 Bloomreach
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

module.exports = (targetOptions, indexHtml) => {
  // Match a link to a css file
  const ngStyle = /<link .*css">/;

  // Match scripts section before closing body tag
  // Capture the loader.js script and closing body tag in groups
  const ngScripts = /(<script.+loader.js.+<\/script>)\s.*(<\/body>)/;

  // Replace the injected css link and remove all angular scripts but leave the loader.js script
  return indexHtml.replace(ngStyle, '').replace(ngScripts, '$1\n  $2');
};
