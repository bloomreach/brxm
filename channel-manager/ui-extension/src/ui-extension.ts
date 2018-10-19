import Penpal from 'penpal';

export interface Parent {
  getProperties: () => Promise<Ui>,
}

export interface Ui {
  user: string,
}

export default class UiExtension {
  static register(onReady: (ui: Ui) => void) {
    if (typeof onReady !== 'function') {
      throw new Error('No callback function provided');
    }

    const parentOrigin = new URLSearchParams(window.location.search).get('br.parentOrigin');
    const connection = Penpal.connectToParent({
      parentOrigin,
    });

    connection.promise.then((parent: Parent) => {
      parent.getProperties().then(onReady);
    });
  }
}

// enable UiExtension.register() in ui-extension.min.js
export const register = UiExtension.register;
