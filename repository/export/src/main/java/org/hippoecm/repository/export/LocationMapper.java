package org.hippoecm.repository.export;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This utility class is responsible for mapping repository paths to context nodes and export files.
 * 
 * Do not call this class from multiple threads. It is not thread safe.
 */
class LocationMapper {

	private final static List<Entry> ENTRIES = new ArrayList<Entry>();
	private final static String NAME = "([\\w:-]+)";
	private final static String ANY = "(.*)";

	// cache the result of the last invocation 
	private static CachedItem LAST_RESULT = new CachedItem(null, null, null);
	
	static {
		// /hippo:namespaces/example
		String[] nodePatterns = new String[] {"/hippo:namespaces/" + NAME};
		String[] propertyPatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME};
		String contextNode = "/hippo:namespaces/$1";
		String file = "namespaces/$1.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hippo:namespaces/example/doctype
		nodePatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME, "/hippo:namespaces/" + NAME + "/" + NAME + "/" + ANY};
		propertyPatterns = new String[] {"/hippo:namespaces/" + NAME + "/" + NAME + "/" + ANY};
		contextNode = "/hippo:namespaces/$1/$2";
		file = "namespaces/$1/$2.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hst:hst/hst:sites
		nodePatterns = new String[] {"/hst:hst/hst:sites" + ANY};
		propertyPatterns = nodePatterns;
		contextNode = "/hst:hst/hst:sites";
		file = "hst/sites.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hst:hst/hst:hosts
		nodePatterns = new String[] {"/hst:hst/hst:hosts" + ANY};
		propertyPatterns = nodePatterns;
		contextNode = "/hst:hst/hst:hosts";
		file = "hst/hosts.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hst:hst/hst:configurations
		nodePatterns = new String[] {"/hst:hst/hst:configurations", "/hst:hst/hst:configurations/" + NAME};
		propertyPatterns = new String[] {"/hst:hst/hst:configurations/" + NAME, "/hst:hst/hst:configurations/" + NAME + "/" + NAME};
		contextNode = "/hst:hst/hst:configurations";
		file = "hst/configurations.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hst:hst/hst:configurations/project
		nodePatterns = new String[] {"/hst:hst/hst:configurations/" + NAME + "/" + NAME + ANY};
		propertyPatterns = nodePatterns;
		contextNode = "/hst:hst/hst:configurations/$1/$2";
		file = "hst/configurations/$1/$2.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hippo:configuration
		nodePatterns = new String[] {"/hippo:configuration", "/hippo:configuration/" + NAME,};
		propertyPatterns = new String[] {"/hippo:configuration/" + NAME, "/hippo:configuration/" + NAME + "/" + NAME,};
		contextNode = "/hippo:configuration";
		file = "configuration.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /hippo:configuration/subnode/subsubnode
		nodePatterns = new String[] {"/hippo:configuration/" + NAME + "/" + NAME, "/hippo:configuration/" + NAME + "/" + NAME + "/" + ANY};
		propertyPatterns = new String[] {"/hippo:configuration/" + NAME + "/" + NAME + "/" + ANY};
		contextNode = "/hippo:configuration/$1/$2";
		file = "configuration/$1/$2.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /content
		nodePatterns = new String[] {"/content", "/content/" + NAME};
		propertyPatterns = new String[] {"/content/" + NAME, "/content/" + NAME + "/" + NAME};
		contextNode = "/content";
		file = "content.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /content/documents/myproject
		nodePatterns = new String[] {"/content/" + NAME + "/" + NAME};
		propertyPatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME};
		contextNode = "/content/$1/$2";
		file = "content/$1/$2.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// /content/documents/myproject/common
		nodePatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME, "/content/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
		propertyPatterns = new String[] {"/content/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
		contextNode = "/content/$1/$2/$3";
		file = "content/$1/$2/$3.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// catch all: /node
		nodePatterns = new String[] {"/" + NAME, "/" + NAME + "/" + NAME};
		propertyPatterns = new String[] {"/" + NAME + "/" + NAME, "/" + NAME + "/" + NAME + "/" + NAME};
		contextNode = "/$1";
		file = "$1.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
		// catch all: /node/subnode/subsubnode
		nodePatterns = new String[] {"/" + NAME + "/" + NAME + "/" + NAME, "/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
		propertyPatterns = new String[] {"/" + NAME + "/" + NAME + "/" + NAME + "/" + ANY};
		contextNode = "/$1/$2/$3";
		file = "$1/$2/$3.xml";
		ENTRIES.add(new Entry(nodePatterns, propertyPatterns, contextNode, file));
	}
	
	static String contextNodeForPath(String path, boolean isNode) {
        if (!path.equals(LAST_RESULT.m_path)) {
            LAST_RESULT = matchPath(path, isNode);
        }
        return LAST_RESULT.m_contextNode;
	}

	static String fileForPath(String path, boolean isNode) {
	    if (!path.equals(LAST_RESULT.m_path)) {
	        LAST_RESULT = matchPath(path, isNode);
	    }
	    return LAST_RESULT.m_file;
	}
	
	private static CachedItem matchPath(String path, boolean isNode) {
        for (Entry entry : ENTRIES) {
            if (isNode) {
                for (Pattern pattern : entry.m_nodePatterns) {
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.matches()) {
                        String contextNode =  entry.m_contextNode;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            contextNode = contextNode.replace("$" + i, matcher.group(i));
                        }
                        String file = entry.m_file;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String qName = matcher.group(i);
                            int indexOfColon = qName.indexOf(':');
                            String name = indexOfColon == -1 ? qName : qName.substring(indexOfColon+1);
                            file = file.replace("$" + i, name);
                        }
                        return new CachedItem(path, contextNode, file);
                    }
                }
            }
            else {
                for (Pattern pattern : entry.m_propertyPatterns) {
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.matches()) {
                        String contextNode = entry.m_contextNode;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            contextNode = contextNode.replace("$" + i, matcher.group(i));
                        }
                        String file = entry.m_file;
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            String qName = matcher.group(i);
                            int indexOfColon = qName.indexOf(':');
                            String name = indexOfColon == -1 ? qName : qName.substring(indexOfColon+1);
                            file = file.replace("$" + i, name);
                        }
                        return new CachedItem(path, contextNode, file);
                    }
                }
            }
        }
        return new CachedItem(null, null, null);
	}
	
	private static final class Entry {
		private final Pattern[] m_nodePatterns;
		private final Pattern[] m_propertyPatterns;
		private final String m_contextNode;
		private final String m_file;
		private Entry(String[] nodePatterns, String[] propertyPatterns, String contextNode, String file) {
			m_nodePatterns = new Pattern[nodePatterns.length];
			for (int i = 0; i < nodePatterns.length; i++) {
				m_nodePatterns[i] = Pattern.compile(nodePatterns[i]);
			}
			m_propertyPatterns = new Pattern[propertyPatterns.length];
			for (int i = 0; i < propertyPatterns.length; i++) {
				m_propertyPatterns[i] = Pattern.compile(propertyPatterns[i]);
			}
			m_contextNode = contextNode;
			m_file = file;
		}
	}
	
	private static final class CachedItem {
		private final String m_path;
		private final String m_contextNode;
		private final String m_file;
		private CachedItem(String path, String contextNode, String file) {
			m_path = path;
			m_contextNode = contextNode;
			m_file = file;
		}
	}
}
