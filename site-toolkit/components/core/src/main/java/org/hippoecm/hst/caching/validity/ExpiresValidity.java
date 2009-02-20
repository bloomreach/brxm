/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.caching.validity;

public final class ExpiresValidity
    implements SourceValidity
{
    
    private static final long serialVersionUID = 1L;
    
    private long expires;
   
    public ExpiresValidity( long expires ) 
    {
        this.expires = System.currentTimeMillis() + expires;
    }

    public int isValid() 
    {
        final long currentTime = System.currentTimeMillis();
        return (currentTime <= this.expires ? SourceValidity.VALID : SourceValidity.INVALID);
    }

    public int isValid( SourceValidity newValidity ) 
    {
        return SourceValidity.INVALID;
    }

    public String toString() 
    {
        return "ExpiresValidity: " + expires;
    }
}
