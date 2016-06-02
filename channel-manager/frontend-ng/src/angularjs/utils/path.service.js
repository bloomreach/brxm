/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
export class PathService {

  concatPaths(...paths) {
    if (!paths) {
      return paths;
    }
    return paths.reduce((result, path) => this._concatTwoPaths(result, path), '');
  }

  _concatTwoPaths(path1, path2) {
    if (!path1 && !path2) {
      return '';
    }
    if (!path1) {
      return path2.trim();
    }
    if (!path2) {
      return path1.trim();
    }

    const path1Trimmed = this._removeTrailingSlashes(path1.trim());
    const path2Trimmed = this._removeLeadingSlashes(path2.trim());
    return `${path1Trimmed}/${path2Trimmed}`;
  }

  _removeTrailingSlashes(path) {
    return path.replace(/\/*$/, '');
  }

  _removeLeadingSlashes(path) {
    return path.replace(/^\/*/, '');
  }

  baseName(path) {
    let result = path;
    if (path) {
      const endIndex = path.endsWith('/') ? path.length - 2 : path.length - 1;
      const lastSlashIndex = path.lastIndexOf('/', endIndex);
      result = path.substring(lastSlashIndex + 1, endIndex + 1);
    }
    return result;
  }
}

