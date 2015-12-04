export class MainCtrl {
  constructor (MainService) {
    "ngInject";
    this.message = MainService.message;
  }
}

