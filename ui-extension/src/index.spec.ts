import { UiExtension } from './index';

let extension: UiExtension;

beforeEach(() => {
  extension = new UiExtension();
});

it('should return test', () => {
  expect(extension.test()).toEqual('test');
});
