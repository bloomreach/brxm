/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { mocked } from 'ts-jest/utils';
import { SimpleChange, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { async, getTestBed, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { destroy, initialize, isPage, Configuration, Page, PageModel } from '@bloomreach/spa-sdk';

import { BrPageComponent } from './br-page.component';

jest.mock('@bloomreach/spa-sdk');

describe('BrPageComponent', () => {
  let component: BrPageComponent;
  let httpMock: HttpTestingController;
  let fixture: ComponentFixture<BrPageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BrPageComponent ],
      imports: [ HttpClientTestingModule ],
      schemas: [ CUSTOM_ELEMENTS_SCHEMA ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    httpMock = getTestBed().get(HttpTestingController);
    fixture = TestBed.createComponent(BrPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    jest.resetAllMocks();
  });

  describe('ngAfterContentChecked', () => {
    it('should sync a page', () => {
      component.state = { sync: jest.fn() } as unknown as Page;
      component.ngAfterContentChecked();

      expect(component.state.sync).toBeCalled();
    });

    it('should not sync a page twice', () => {
      component.state = { sync: jest.fn() } as unknown as Page;
      component.ngAfterContentChecked();
      component.ngAfterContentChecked();

      expect(component.state.sync).toBeCalledTimes(1);
    });

    it('should not fail if the page is not ready', () => {
      expect(() => component.ngAfterContentChecked()).not.toThrow();
    });
  });

  describe('ngOnChanges', () => {
    it('should use a page instance from inputs when configuraton is changed', () => {
      mocked(isPage).mockReturnValueOnce(true);
      component.configuration = {} as Configuration;
      component.page = {} as Page;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
        page: new SimpleChange(undefined, component.page, true),
      });

      expect(initialize).not.toBeCalled();
      expect(component.state).toBe(component.page);
    });

    it('should destroy a previous page', () => {
      const previousPage = {} as Page;

      mocked(isPage).mockReturnValueOnce(true);
      component.configuration = {} as Configuration;
      component.page = {} as Page;
      component.state = previousPage;
      component.ngOnChanges({
        configuration: new SimpleChange({}, component.configuration, false),
        page: new SimpleChange(previousPage, component.page, false),
      });

      expect(destroy).toBeCalledWith(previousPage);
    });

    it('should initialize a new page when a page input was not changed', () => {
      mocked(initialize).mockResolvedValueOnce({} as Page);
      component.configuration = {} as Configuration;
      component.page = {} as Page;
      component.ngOnChanges({
        configuration: new SimpleChange({}, component.configuration, false),
      });

      expect(initialize).toBeCalled();
    });

    it('should use a page instance from inputs when configuration was not changed', () => {
      mocked(isPage).mockReturnValueOnce(true);
      component.page = {} as Page;
      component.ngOnChanges({
        page: new SimpleChange(undefined, component.page, true),
      });

      expect(initialize).not.toBeCalled();
      expect(component.state).toBe(component.page);
    });

    it('should initialize a page from the configuration', async () => {
      const page = {} as Page;

      mocked(initialize).mockResolvedValueOnce(page);
      component.configuration = { cmsBaseUrl: 'something' } as Configuration;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
      });

      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith(
        expect.objectContaining({
          cmsBaseUrl: 'something',
          httpClient: expect.any(Function),
        }),
        undefined,
      );
      expect(component.state).toBe(page);
    });

    it('should initialize a page from the page model', async () => {
      const page = {} as Page;

      mocked(initialize).mockResolvedValueOnce(page);
      component.configuration = { cmsBaseUrl: 'something' } as Configuration;
      component.page = {} as PageModel;
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, component.configuration, true),
        page: new SimpleChange(undefined, component.page, true),
      });

      await new Promise(process.nextTick);

      expect(initialize).toBeCalledWith(expect.any(Object), component.page);
      expect(component.state).toBe(page);
    });

    it('should pass a compatible http client', () => {
      mocked(initialize).mockResolvedValueOnce({} as Page);
      component.ngOnChanges({
        configuration: new SimpleChange(undefined, {}, true),
      });

      const [[{ httpClient }]] = mocked(initialize).mock.calls;
      const response = httpClient({
        data: 'something',
        headers: { 'Some-Header': 'value' },
        method: 'POST',
        url: 'http://www.example.com',
      });
      const request = httpMock.expectOne('http://www.example.com');

      expect(request.request.headers.get('Some-Header')).toBe('value');
      expect(request.request.body).toBe('something');
      expect(request.request.method).toBe('POST');
      expect(request.request.url).toBe('http://www.example.com');

      request.flush('something');

      expect(response).resolves.toEqual({ data: 'something' });
    });
  });

  describe('ngOnDestroy', () => {
    it('should destroy a stored page', () => {
      const page = {} as Page;
      component.state = page;
      component.ngOnDestroy();

      expect(destroy).toBeCalledWith(page);
    });

    it('should not destroy a page if it was not initialized', () => {
      component.ngOnDestroy();

      expect(destroy).not.toBeCalled();
    });
  });
});
