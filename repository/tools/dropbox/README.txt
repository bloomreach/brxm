How to run..

Currently only running in eclipse is possible.
Make sure you have a repository running before starting dropbox/mp3box.

Basically follow the steps from: http://repodocs.hippocms.org/extending-hippo-ecm/building/eclipse.html

The short list:
- add rmi plugin to eclipse
- add rmi to project
- right click on java file -> run as rmi..
- arguments tab
--- Program arguments: rmi://localhost:1099/jackrabbit.repository <local mp3s> <username> <password>
--- VM Arguments: -Djava.security.manager
- rmi settings tab
--- add first two, set java.security.police -> select client.policy file from project
--- set java.rmi.server.codebase -> autogenerate
- apply
- run!

