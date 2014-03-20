//############################################
// USING
//############################################
* add  /essentials/ide/intellij/project-setup/ide-integration.jar to ~/.IntelliJIdea13/config/plugins folder
* restart Intellij and use: File > New project > Hippo Essentials plugin


//############################################
// BUILDING PLUGIN
//############################################


* create new intellij SDK (intellij 13+)
* add new module to your project: /essentials/ide/intellij/project-setup/ide-integration.iml
* change module SDK (to the one your created in first step)
* make sure compiler patterns doesn't filter template files (add *.ft to compiler's resource patterns)