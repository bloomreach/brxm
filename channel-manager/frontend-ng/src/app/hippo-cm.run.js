function run(NavappService) {
  'ngInject';

  NavappService.connect();

  const event = new Event('angularjsInjectorReady');
  document.body.dispatchEvent(event);
}

export default run;
