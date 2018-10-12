import { Extension } from './index';

let extension: Extension;

beforeEach(() => {
  extension = new Extension();
});

it('should return test', () => {
  expect(extension.test()).toEqual('test');
});
