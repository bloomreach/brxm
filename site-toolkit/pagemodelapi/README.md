How to create a new version of the API
======================================

When creating a *new* Page Model API version you have to copy all java classes and resources
from the previous version to a new MVN module ***and*** make sure that the 
lastest version classes/resources contain the GIT revision history.

Thus for example, when creating Version 1.0, **rename** the current 'pagemodelapi-v09' MVN 
module to 'pagemodelapi-v10' and move the package names to 'pagemodelapi.v10'. After this,
copy the 'pagemodelapi-v10' to 'pagemodelapi-v09' and move for the new MVN module the 
packages again to 'pagemodelapi.v09'. Now make sure that all GIT version history is present
in the MVN 'pagemodelapi-v10' 

    Always make sure the GIT revision history is maintained in the latest version!
    
The reason for this is that older versions at some point will be removed. We do not want
to loose the GIT revision history however of course
  
Apart from the steps in this project, you might have to take similar steps in downstream
(possibly enterprise) projects