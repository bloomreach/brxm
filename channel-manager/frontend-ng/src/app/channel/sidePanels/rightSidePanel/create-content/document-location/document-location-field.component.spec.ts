/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';
import 'rxjs/add/observable/of';

import { DocumentLocationFieldComponent } from './document-location-field.component';
import FeedbackService from '../../../../../services/feedback.service.js';
import { HintsComponent } from '../../../../../shared/components/hints/hints.component';
import { SharedModule } from '../../../../../shared/shared.module';
import CreateContentService from '../createContent.service.js';
import { ChannelServiceMock, CreateContentServiceMock, FeedbackServiceMock } from '../create-content.mocks.spec';
import ChannelService from '../../../../channel.service.js';

describe('DocumentLocationField Component', () => {
  let component: DocumentLocationFieldComponent;
  let fixture: ComponentFixture<DocumentLocationFieldComponent>;
  let createContentService: CreateContentService;
  let getFolderSpy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        DocumentLocationFieldComponent,
        HintsComponent
      ],
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        SharedModule
      ],
      providers: [
        { provide: ChannelService, useClass: ChannelServiceMock },
        { provide: CreateContentService, useClass: CreateContentServiceMock },
        { provide: FeedbackService, useClass: FeedbackServiceMock },
      ]
    });

    fixture = TestBed.createComponent(DocumentLocationFieldComponent);
    component = fixture.componentInstance;
    createContentService = fixture.debugElement.injector.get(CreateContentService);
    getFolderSpy = spyOn(createContentService, 'getFolders').and.callThrough();
  });

  describe('parsing the rootPath @Input', () => {
    it('defaults to the channel root if not set', () => {
      fixture.detectChanges();
      expect(component.rootPath).toBe('/channel/content');
    });

    it('overrides the channel root path if absolute', () => {
      component.rootPath = '/root/path';
      fixture.detectChanges();
      expect(component.rootPath).toBe('/root/path');
    });

    it('is concatenated wth the channel\'s root path if relative', () => {
      component.rootPath = 'some/path';
      fixture.detectChanges();
      expect(component.rootPath).toBe('/channel/content/some/path');
    });

    it('never ends with a slash', () => {
      component.rootPath = '/root/path/';
      fixture.detectChanges();
      expect(component.rootPath).toBe('/root/path');

      component.rootPath = 'some/path/';
      component.ngOnInit();
      expect(component.rootPath).toBe('/channel/content/some/path');
    });

    it('detects the root path depth', () => {
      component.rootPath = '/root';
      fixture.detectChanges();
      expect(component.rootPathDepth).toBe(1);

      component.rootPath = '/root/path/';
      component.ngOnInit();
      expect(component.rootPathDepth).toBe(2);

      component.rootPath = 'some/path/';
      component.ngOnInit();
      expect(component.rootPathDepth).toBe(4);
    });
  });

  describe('parsing the defaultPath @Input', () => {
    it('throws an error if defaultPath is absolute', () => {
      component.defaultPath = '/path';
      expect(() => fixture.detectChanges()).toThrow(new Error('The defaultPath option can only be a relative path'));
    });
  });

  describe('setting the document location', () => {
    it('stores the path of the last folder returned by the create-content-service', fakeAsync(() => {
      const folders = [{path: '/root'}, {path: '/root/path'}];
      getFolderSpy.and.returnValue(Promise.resolve(folders));
      component.ngOnInit();
      tick();
      expect(component.documentLocation).toBe('/root/path');
    }));

    it('stores the value of defaultPath returned by the create-content-service', fakeAsync(() => {
      component.rootPath = '/root';
      const folders = [{name: 'root'}, {name: 'default'}, {name: 'path'}];
      getFolderSpy.and.returnValue(Promise.resolve(folders));
      component.ngOnInit();
      tick();
      expect(component.defaultPath).toBe('default/path');
    }));
  });

  describe('setting the document location label', () => {
    const setup = (rootPath, defaultPath, displayNames) => {
      const folders = [];
      displayNames.forEach((displayName) => {
        folders.push({ displayName, path: '' });
      });
      getFolderSpy.and.returnValue(Promise.resolve(folders));

      component.rootPath = rootPath;
      component.defaultPath = defaultPath;
      component.ngOnInit();
      tick();
    };

    it('uses displayName(s) for the document location label', fakeAsync(() => {
      setup('/root', '', ['R00T']);
      expect(component.documentLocationLabel).toBe('R00T');

      setup('/root', 'bloom', ['R00T', 'bl00m']);
      expect(component.documentLocationLabel).toBe('R00T/bl00m');
    }));

    it('uses only one folder of root path if default path is empty', fakeAsync(() => {
      setup('', '', ['channel', 'content']);
      expect(component.documentLocationLabel).toBe('content');

      setup('root', '', ['channel', 'content', 'root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('/root', '', ['root']);
      expect(component.documentLocationLabel).toBe('root');

      setup('root/path', '', ['channel', 'content', 'root', 'path']);
      expect(component.documentLocationLabel).toBe('path');

      setup('/root/path', '', ['root', 'path']);
      expect(component.documentLocationLabel).toBe('path');
    }));

    it('uses only one folder of root path if default path depth is less than 3', fakeAsync(() => {
      setup('', 'some', ['channel', 'content', 'some']);
      expect(component.documentLocationLabel).toBe('content/some');

      setup('', 'some/folder', ['channel', 'content', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('content/some/folder');

      setup('root', 'some/folder', ['channel', 'content', 'root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');

      setup('/root', 'some/folder', ['root', 'some', 'folder']);
      expect(component.documentLocationLabel).toBe('root/some/folder');
    }));

    it('always shows a maximum of 3 folders', fakeAsync(() => {
      setup('root', 'folder/with/document', ['channel', 'content', 'root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/document', ['root', 'folder', 'with', 'document']);
      expect(component.documentLocationLabel).toBe('folder/with/document');

      setup('/root', 'folder/with/some/document', ['root', 'folder', 'with', 'some', 'document']);
      expect(component.documentLocationLabel).toBe('with/some/document');

      setup('/root', 'folder/with/some/nested/document', ['root', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');

      setup('/root/path', 'folder/with/some/nested/document', ['root', 'path', 'folder', 'with', 'some', 'nested', 'document']);
      expect(component.documentLocationLabel).toBe('some/nested/document');
    }));
  });

  describe('the locale @Output', () => {
    it('emits the locale when component is initialized', fakeAsync(() => {
      let changedLocale: string;
      component.changeLocale.subscribe((locale: string) => changedLocale = locale);

      const folders = [{path: '/root'}, {path: '/root/path', locale: 'de'}];
      getFolderSpy.and.returnValue(Promise.resolve(folders));
      component.ngOnInit();
      tick();
      expect(changedLocale).toBe('de');
    }));
  });
});
