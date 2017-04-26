class modeToggleController {
  $onInit() {
  }

  toggleContentMode() {
    this.mode.content = !this.mode.content;
  }

  toggleComponentsMode() {
    this.mode.components = !this.mode.components;
  }
}

export default modeToggleController;
