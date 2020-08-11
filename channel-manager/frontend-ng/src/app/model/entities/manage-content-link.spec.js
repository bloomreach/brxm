/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { ManageContentLink } from './manage-content-link';

describe('ManageContentLink', () => {
  describe('getType', () => {
    it('should return manage-content-link type', () => {
      const manageContentLink = new ManageContentLink({});

      expect(manageContentLink.getType()).toBe('manage-content-link');
    });
  });

  describe('getDefaultPath', () => {
    it('should return default path', () => {
      const manageContentLink = new ManageContentLink({ defaultPath: 'default-path' });

      expect(manageContentLink.getDefaultPath()).toBe('default-path');
    });
  });

  describe('getParameterName', () => {
    it('should return a parameter name', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'parameter-name' });

      expect(manageContentLink.getParameterName()).toBe('parameter-name');
    });
  });

  describe('getParameterValue', () => {
    it('should return a parameter value', () => {
      const manageContentLink = new ManageContentLink({ parameterValue: 'parameter-value' });

      expect(manageContentLink.getParameterValue()).toBe('parameter-value');
    });
  });

  describe('isParameterValueRelativePath', () => {
    it('should return true when parameterValueIsRelativePath is true', () => {
      const manageContentLink = new ManageContentLink({ parameterValueIsRelativePath: 'true' });

      expect(manageContentLink.isParameterValueRelativePath()).toBe(true);
    });

    it('should return false when parameterValueIsRelativePath is omitted', () => {
      const manageContentLink = new ManageContentLink({});

      expect(manageContentLink.isParameterValueRelativePath()).toBe(false);
    });
  });

  describe('getPickerConfig', () => {
    it('should return null when the parameter name is omitted', () => {
      const manageContentLink = new ManageContentLink({});

      expect(manageContentLink.getPickerConfig()).toBeNull();
    });

    it('should return an object containing picker configuration', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name', pickerConfiguration: 'config' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ configuration: 'config' }));
    });

    it('should return an object containing an initial path', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name', pickerInitialPath: 'path' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ initialPath: 'path' }));
    });

    it('should return an object containing false in isRelativePath property', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ isRelativePath: false }));
    });

    it('should return an object containing a flag of remembering the last visit', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name', pickerRemembersLastVisited: 'true' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ remembersLastVisited: true }));
    });

    it('should return an object containing root path', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name', pickerRootPath: 'path' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ rootPath: 'path' }));
    });

    it('should return an object containing an empty array of types when selectable node types are omitted', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({ selectableNodeTypes: [] }));
    });

    it('should return an object containing an array of selectable node types', () => {
      const manageContentLink = new ManageContentLink({ parameterName: 'name', pickerSelectableNodeTypes: 'a,b' });

      expect(manageContentLink.getPickerConfig()).toEqual(jasmine.objectContaining({
        selectableNodeTypes: ['a', 'b'],
      }));
    });
  });

  describe('getRootPath', () => {
    it('should return a root path', () => {
      const manageContentLink = new ManageContentLink({ rootPath: 'path' });

      expect(manageContentLink.getRootPath()).toBe('path');
    });
  });

  describe('getDocumentTemplateQuery', () => {
    it('should return a document template query', () => {
      const manageContentLink = new ManageContentLink({ documentTemplateQuery: 'query' });

      expect(manageContentLink.getDocumentTemplateQuery()).toBe('query');
    });
  });

  describe('getFolderTemplateQuery', () => {
    it('should return a folder template query', () => {
      const manageContentLink = new ManageContentLink({ folderTemplateQuery: 'query' });

      expect(manageContentLink.getFolderTemplateQuery()).toBe('query');
    });
  });
});
