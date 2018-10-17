import Penpal from 'penpal';

// TODO: do we need this class? maybe for the ui-extension.js file?
export class UiExtension {
  register(onReady: Function) {
    // TODO: read origin of CMS from query parameter in the URL and pass it to Penpal as the parentOrigin
    const connection = Penpal.connectToParent();

    connection.promise.then(parent => {
      // TODO: add type for 'cms' object
      parent.getCmsProperties().then((cms: object) => {
        onReady(cms);
      });
    });
  }
}

export function register(onReady: Function) {
  return new UiExtension().register(onReady);
}
