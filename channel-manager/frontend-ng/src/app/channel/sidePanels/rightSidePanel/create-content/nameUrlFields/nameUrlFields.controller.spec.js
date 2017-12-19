// /*
//  * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *  http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */
//
// describe('DocumentLocationField', () => {
//   let $componentController;
//   let $q;
//   let $rootScope;
//   let ChannelService;
//   let CreateContentService;
//   let FeedbackService;
//
//   let component;
//   let $scope;
//   let getFolderSpy;
//   let getChannelSpy;
//
//   beforeEach(() => {
//     angular.mock.module('hippo-cm.channel.createContentModule');
//
//     inject((_$componentController_, _$q_, _$rootScope_, _ChannelService_, _CreateContentService_, _FeedbackService_) => {
//       $componentController = _$componentController_;
//       $q = _$q_;
//       $rootScope = _$rootScope_;
//       ChannelService = _ChannelService_;
//       CreateContentService = _CreateContentService_;
//       FeedbackService = _FeedbackService_;
//     });
//
//     $scope = $rootScope.$new();
//     component = $componentController('documentLocationField');
//
//     getFolderSpy = spyOn(CreateContentService, 'getFolders').and.returnValue($q.resolve());
//     getChannelSpy = spyOn(ChannelService, 'getChannel').and.returnValue({ contentRoot: '/channel/content' });
//     component.changeLocale = () => angular.noop();
//   });
//
//   afterEach(() => {
//     delete component.form.controls['name'];
//   });
//
//   function setNameInputValue (value) {
//     const nameInput = component.nameInputElement.nativeElement;
//     component.form.controls.name.setValue(value);
//     component.nameField = value;
//     nameInput.dispatchEvent(new Event('keyup'));
//   }
//
//   describe('ngOnInit', () => {
//     it('calls setDocumentUrlByName 1 second after keyup was triggered on nameInputElement', fakeAsync(() => {
//       setNameInputValue('test val');
//       tick(1000);
//       expect(component.setDocumentUrlByName).toHaveBeenCalled();
//     }));
//
//     it('sets the url with locale automatically after locale has been changed', fakeAsync(() => {
//       setNameInputValue('some val');
//       tick(1000);
//       expect(component.setDocumentUrlByName).toHaveBeenCalled();
//       expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'en');
//
//       hostComponent.locale = 'de';
//       hostFixture.detectChanges();
//       expect(component.setDocumentUrlByName).toHaveBeenCalled();
//       expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'de');
//     }));
//   });
//
//   describe('setDocumentUrlByName', () => {
//     it('calls CreateContentService.generateDocumentUrlByName and applies the new url', () => {
//       setNameInputValue('test');
//       // component.setDocumentUrlByName();
//       // expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test', 'en');
//       // expect(component.urlField).toEqual('test');
//       //
//       // spies.generateDocumentUrlByName.calls.reset();
//       //
//       // setNameInputValue('test val');
//       // component.setDocumentUrlByName();
//       // expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test val', 'en');
//       // expect(component.urlField).toEqual('test-val');
//     });
//
//     it('changes the URL upon editing the name, as long as isManualUrlMode is false', fakeAsync(() => {
//       // Name editing triggers generation of URL from the back-end
//       setNameInputValue('First edit');
//       tick(1000);
//       expect(spies.generateDocumentUrlByName).toHaveBeenCalled();
//       expect(component.urlField).toEqual('first-edit');
//
//       // Manual editing of the URL
//       component.setManualUrlEditMode(true);
//       component.urlField = 'manual-edit-of-url';
//
//       spies.generateDocumentUrlByName.calls.reset();
//
//       // Until manual editing mode is disabled, URL generations should be bypassed
//       setNameInputValue('Second edit, should not change the URL');
//       tick(1000);
//       expect(spies.generateDocumentUrlByName).not.toHaveBeenCalled();
//       expect(component.urlField).toEqual('manual-edit-of-url');
//     }));
//   });
//
//   describe('validateFields', () => {
//     describe('conditions scenarios', () => {
//       it('returns true, all conditions resolved to "true"', () => {
//         component.nameField = 'name';
//         component.urlField = 'url';
//         expect(component.validateFields()).toEqual(true);
//       });
//
//       it('returns false, name field is empty (conditions index 0)', () => {
//         component.nameField = '';
//         component.urlField = 'url';
//         expect(component.validateFields()).toEqual(false);
//       });
//
//       it('returns false, url field is empty (conditions index 1)', () => {
//         component.nameField = 'name';
//         component.urlField = '';
//         expect(component.validateFields()).toEqual(false);
//       });
//
//       it('returns false, name field is only whitespace(s) (conditions index 2)', () => {
//         component.nameField = '     ';
//         component.urlField = 'url';
//         expect(component.validateFields()).toEqual(false);
//       });
//
//       it('returns false, url field is only whitespace(s) (conditions index 3)', () => {
//         component.nameField = 'name';
//         component.urlField = '     ';
//         expect(component.validateFields()).toEqual(false);
//       });
//     });
//   });
// });
