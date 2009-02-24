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
package org.hippoecm.hst.core.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * <h2>Overview</h2>
 * <p>
 * The Path object is used to standard used to standardize the creation of
 * mutation of path-like structures. For: example /foo/bar/index.html.
 * </p>
 * <h2>Rules for Interperting Pathes</h2>
 * <p>
 * Below are the rules for how the constructor interprets literal paths.
 * <strong>NOTE</strong> the {@link addSegment(String)} interprets string
 * pathes in a somewhat different manner. <table>
 * <tr>
 * <th>Literal Path</th>
 * <th>Interpretation</th>
 * </tr>
 * <td> <i>/foo/bar/index.html</i> </td>
 * <td> <code>foo</code> and <code>bar</code> will be considered directory
 * segments while <code>index.html</code> will be considered a file segment.
 * This means that the <code>baseName</code> will be set to <i>index</i> and
 * the <code>fileExtension</code> will be set to <i>.html</i> </td>
 * <tr>
 * <td> <i>/foo/bar/</i>, <i>/foo/bar</i>, <i>foo/bar/</i> <i>foo/bar</i>
 * </td>
 * <td>
 * <p>
 * <code>foo</code> and <code>bar</code> will be considered directory
 * segments. <code>baseName</code> and <code>fileExtension</code> will be
 * left as <code>null</code>.
 * <p>
 * I cases where a file has no extension you must use the
 * {@link setFileSegment(String))} to manually set the file. This causes the
 * <code>baseName</code> to be set to the file name specified and the
 * <code>fileExtension</code> will be set to the empty string ("").
 * </p>
 * </td>
 * </tr>
 * </table>
 * 
 * @version $Id$
 */
public class Path implements Serializable, Cloneable
{
    /** The serial version uid. */
    private static final long serialVersionUID = 6890966283704092945L;

    public static final String PATH_SEPERATOR = "/";
    
    private static final String[] EMPTY_SEGMENTS = new String[0];
    
    private static HashMap childrenMap = new HashMap();
    
    private final String path;
    private final String[] segments;

    private final String fileName;

    private final String baseName;

    private final String fileExtension;

    private final String queryString;
    
    private final int hashCode;

    public Path()
    {
        segments = EMPTY_SEGMENTS;
        fileName = null;
        baseName = null;
        fileExtension = null;
        queryString = null;
        hashCode = 0;
        path = "";
    }
    
    private Path(Path parent, String childSegment, boolean pathOnly)
    {
        this(parent, splitPath(childSegment), pathOnly);
    }
            
    private Path(Path parent, String[] children, boolean pathOnly)
    {
        int code = 0;
        if (!pathOnly)
        {
            this.fileName = parent.fileName;
            this.baseName = parent.baseName;
            this.fileExtension = parent.fileExtension;
            this.queryString = parent.queryString;
            if (queryString != null)
            {
                code += queryString.hashCode();
            }
        }
        else
        {
            fileName = null;
            baseName = null;
            fileExtension = null;
            queryString = null;
        }
        
        int size = parent.segments.length;
        if (pathOnly && parent.fileName != null)
        {
            size--;
        }

        int index = 0;
        
        segments = new String[size+children.length];
        for (index = 0; index < size; index++)
        {
            segments[index] = parent.segments[index];
            code += segments[index].hashCode();
        }
        for (int i = 0; i < children.length; i++, index++)
        {
            segments[index] = children[i];
            code += segments[index].hashCode();
        }
        if (fileName != null && !pathOnly)
        {
            segments[index] = fileName;
            code += segments[index].hashCode();
        }
        hashCode = code;
        path = buildPath();
    }
            
    private Path(Path parent)
    {
        this.fileName = parent.fileName;
        this.baseName = parent.baseName;
        this.fileExtension = parent.fileExtension;
        this.queryString = parent.queryString;
        segments = new String[parent.segments.length-1];
        int code = 0;
        for (int i = 0; i < parent.segments.length-2; i++)
        {
            segments[i] = parent.segments[i];
            code += segments.hashCode();
        }
        if (fileName != null)
        {
            segments[segments.length-1] = fileName;
        }
        else if (parent.segments.length > 1)
        {
            segments[segments.length-1] = parent.segments[parent.segments.length-2];
        }
        if ( segments.length > 0)
        {
            code += segments[segments.length-1].hashCode();
        }
        if (queryString != null)
        {
            code += queryString.hashCode();
        }
        hashCode = code;
        path = buildPath();
    }
            
    private Path(String[] segments, int offset, int count)
    {
        this.segments = new String[count];
        int code = 0;
        for (int i = 0; i < count; i++)
        {
            this.segments[i] = segments[offset+i];
            code+=segments[offset+i].hashCode();
        }
        hashCode = code;
        if (count > 0)
        {
            fileName = this.segments[count-1];
            int extIndex = fileName.lastIndexOf('.');
            if (extIndex > -1)
            {
                baseName = fileName.substring(0, extIndex);
                fileExtension = fileName.substring(extIndex);
            }
            else
            {
                baseName = fileName;
                fileExtension = "";
            }
        }
        else
        {
            fileName = null;
            baseName = null;
            fileExtension = null;
        }
        queryString = null;
        path = buildPath();
    }
            
    public Path(String path)
    {
        
        String tmp = path.replace('\\', '/');

        if (!tmp.startsWith("/"))
        {
            tmp = "/" + tmp;
        }

        this.path = tmp;
        
        if (path.equals("/"))
        {
            segments = new String[]{""};
            fileName = null;
            baseName = null;
            fileExtension = null;
            queryString = null;
            hashCode = 0;
        }
        else
        {
            int queryStart = path.indexOf('?');
            int len = queryStart > -1 ? queryStart : path.length();
            segments = split(path, 0, len, '/');
            int code  = 0;
            for (int i = 0; i < segments.length; i++)
            {
                code += segments[i].hashCode();
            }
            String tmpFileName = null;
            
            if (queryStart > 1 && path.length() > queryStart+1)
            {
                queryString = path.substring(queryStart+1);
                code += queryString.hashCode();
            }
            else
            {
                queryString = null;
            }
            hashCode = code;
            int extIndex = -1;
            if (segments.length > 0)
            {
                tmpFileName = segments[segments.length-1];
                extIndex = tmpFileName.lastIndexOf('.');
            }
            if (extIndex > -1)
            {
                fileName = tmpFileName;
                baseName = tmpFileName.substring(0, extIndex);
                fileExtension = tmpFileName.substring(extIndex);
            }
            else
            {
                fileName = null;
                baseName = null;
                fileExtension = null;
            }
        }
    }

    private static String[] splitPath(String path)
    {
        String[] children = null;
        path = path.replace('\\', '/');

        if (path.startsWith("/"))
        {
            path = "/" + path;
        }

        if (path.equals("/"))
        {
            children = new String[]{""};
        }
        else
        {
            int index = path.indexOf('?');
            int len = index > -1 ? index : path.length();
            children = split(path, 0, len, '/');
        }
        return children;
    }
    
    /**
     * Returns the segement of the path at the specified index <code>i</code>.
     * 
     * @param i
     *            index containing the segment to return.
     * @return Segment at index <code>i</code>
     * @throws ArrayIndexOutOfBoundsException
     *             if the index is not within the bounds of this Path.
     */
    public String getSegment(int i)
    {
        return segments[i];
    }

    /**
     * <p>
     * Adds this segment to the end of the path but before the current file
     * segment, if one exists. For consistency Segments added via this method
     * are <strong>ALWAYS</strong> considered directories even when matching a
     * standrad file pattern i.e. <i>index.html</i>
     * </p>
     * <p>
     * If you need to set the file segment, please use the setFileSegment()
     * method.
     * </p>
     * 
     * @param segment
     * @return
     */
    public Path addSegment(String segment)
    {
        return new Path(this, segment, false);
    }

    public Path getSubPath(int beginAtSegment)
    {
        return new Path(segments, beginAtSegment, segments.length-beginAtSegment);
    }

    public Path getSubPath(int beginAtSegment, int endSegment)
    {
        return new Path(segments, beginAtSegment, endSegment-beginAtSegment);
    }

    public String getBaseName()
    {
        return baseName;
    }

    public String getFileExtension()
    {
        return fileExtension;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getQueryString()
    {
        return queryString;
    }

    public int length()
    {
        return segments.length;
    }
    
    public String toString()
    {
        return path;
    }

    private String buildPath()
    {
        int len = 0;
        for (int i = 0; i < segments.length; i++)
        {
            len+=segments[i].length()+1;
        }
        if (queryString!=null)
        {
            len+=queryString.length()+1;
        }
        char[] buffer = new char[len];
        int index = 0;
        for (int i = 0; i < segments.length; i++ )
        {
            buffer[index++] = '/';
            len = segments[i].length();
            segments[i].getChars(0, len, buffer, index);
            index+= len;
        }
        if (queryString != null)
        {                
            buffer[index++] = '?';
            len = queryString.length();
            queryString.getChars(0, len, buffer, index);
        }
        return new String(buffer);
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof Path)
        {
            Path other = (Path)obj;
            if ( (other.queryString != null && other.queryString.equals(queryString)) ||
                    (other.queryString == null && queryString == null) )
            {
                return Arrays.equals(other.segments,segments);
            }
        }
        return false;
    }

    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Removes the last directory segment in this path. This method <strong>WILL
     * NOT</strong> remove the fileSegment, but path segment immediately before
     * it.
     * 
     * @return segment removed.
     */
    public Path removeLastPathSegment()
    {
        if ((fileName != null && segments.length == 1) || segments.length == 0)
        {
            return this;
        }
        return new Path(this);
    }

    public Path getChild(String childPath)
    {
        synchronized (childrenMap)
        {
            Path child = null;
            HashMap children = (HashMap)childrenMap.get(path);
            if (children == null)
            {
                children = new HashMap();
                childrenMap.put(path, children);
            }
            else
            {
                child = (Path)children.get(childPath);
            }
            if ( child == null )
            {
                if (segments.length == 0)
                {
                    child = new Path(childPath);
                }
                else
                {
                    child = new Path(this, childPath, true);
                }
                children.put(childPath, child);
            }
            return child;
        }
    }

    public Path getChild(Path childPath)
    {
        return getChild(childPath.path);
    }
    
    private static String[] split(String str, int start, int length, char separator)
    {
        String[] result;
        char[] buffer = str.toCharArray();
        int tokens = 0;
        boolean token = false;
        for (int i = start; i < length; i++)
        {
            if (buffer[i]==separator)
            {
                token = false;
            }
            else if (!token)
            {
                tokens++;
                token = true;
            }
        }
        result = new String[tokens];
        if (tokens > 0)
        {
            int begin = start;
            int end = start;
            token = false;
            tokens = 0;
            for (int i = start; i < length; i++)
            {
                if (buffer[i]==separator)
                {
                    if (token)
                    {
                       result[tokens++] = new String(buffer,begin,end);
                       token = false;
                    }
                }
                else if (!token)
                {
                    token = true;
                    begin = i;
                    end = 1;
                }
                else
                {
                    end++;
                }
            }
            if (token)
            {
                result[tokens] = new String(buffer,begin, end);
            }
        }
        return result;
    }
}
