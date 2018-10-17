export class UiExtension {
  test() {
    return 'test';
  }
}

export function register() {
  return new UiExtension();
}
