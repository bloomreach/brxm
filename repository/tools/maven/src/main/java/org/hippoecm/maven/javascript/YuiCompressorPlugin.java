package org.hippoecm.maven.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Maven plugin that compresses YUI javascript modules, code based on org.codehaus.mojo.javascript classes
 * 
 * @goal compress
 * @requiresDependencyResolution runtime
 * @phase compile
 */
public class YuiCompressorPlugin extends AbstractMojo {

    private static final NumberFormat INTEGER = NumberFormat.getIntegerInstance();

    private static final String[] DEFAULT_INCLUDES = new String[] { "**/*.js" };

    /**
     * Inclusion patterns
     * 
     * @parameter
     */
    private String[] includes;

    private static final String[] DEFAULT_EXCLUDES = new String[] { "**/*-debug.js", "**/*-min.js" };

    /**
     * Exclusion patterns
     * 
     * @parameter
     */
    private String[] excludes;

    /**
     * Location of the scripts files.
     * 
     * @parameter default-value="${project.build.outputDirectory}"
     */
    private File scriptsDirectory;
    
    
    //YuiCompressor options

    /**
     * Insert a line break after the specified column number
     * 
     * @parameter default-value=-1
     */
    private int linebreakPos;

    /**
     * If set to false do not obfuscate (minify only)
     * 
     * @parameter default-value=false
     */
    private boolean munge;

    /**
     * Display informational messages and warnings
     * 
     * @parameter default-value=false
     */
    private boolean verbose;

    /**
     * Preserve all semicolons
     * 
     * @parameter default-value=true
     */
    private boolean preserveAllSemiColons;

    /**
     * Disable all micro optimizations
     * 
     * @parameter default-value=false
     */
    private boolean disableOptimizations;
    

    public void execute() throws MojoExecutionException {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(scriptsDirectory);
        if (includes == null) {
            includes = DEFAULT_INCLUDES;
        }
        scanner.setIncludes(includes);
        if (excludes == null) {
            excludes = DEFAULT_EXCLUDES;
        }
        scanner.setExcludes(excludes);
        scanner.addDefaultExcludes();

        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        scriptsDirectory.mkdirs();
        long saved = 0;
        for (int i = 0; i < files.length; i++) {
            String file = files[i];
            if (isYuiModule(file)) {
                saved += compress(file);
            }
        }
        getLog().info("Compression saved " + INTEGER.format(saved) + " bytes");
    }

    private boolean isYuiModule(String filename) {
        InputStream is;
        File file = new File(scriptsDirectory, filename);
        try {
            is = new FileInputStream(file);
        } catch (FileNotFoundException e1) {
            getLog().error("File " + filename + " not found");
            return false;
        }
        BufferedReader buffy = new BufferedReader(new InputStreamReader(is));
        try {
            String line;
            Pattern pattern = Pattern.compile("\\* @module (.*)");

            while ((line = buffy.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String module = matcher.group(1).trim();
                    String moduleFilename = module + ".js";
                    if (file.getName().equals(moduleFilename)) {
                        return true;
                    }
                    break;
                }
            }
        } catch (IOException e) {
            getLog().error("Error checking if " + filename + " + is a YUI module", e);
        } finally {
            try {
                buffy.close();
            } catch (IOException e) {
                getLog().error("Enable to close BufferedReader", e);
            }
        }
        return false;
    }

    private long compress(String file) throws MojoExecutionException {
        int ext = file.lastIndexOf('.');
        String minName = file.substring(0, ext) + "-" + "min" + file.substring(ext);
        File compressed = new File(scriptsDirectory, minName);
        compressed.getParentFile().mkdirs();

        return compress(new File(scriptsDirectory, file), compressed);
    }
    
    private long compress(File in, File compressed) throws MojoExecutionException {
        if (in.length() > 0) {
            FileWriter out = null;
            try {
                out = new FileWriter(compressed);
                JavaScriptCompressor c = new JavaScriptCompressor(new FileReader(in), new MojoErrorReporter());
                c.compress(out, linebreakPos, munge, verbose, preserveAllSemiColons, disableOptimizations);
            } catch (IOException e) {
                throw new MojoExecutionException("Error compressing source[" + in.getAbsolutePath() + "]", e);
            } finally {
                IOUtil.close(out);
            }
            String describe = in.getName() + " (in: " + INTEGER.format(in.length()) + " bytes, out:"
                    + INTEGER.format(compressed.length()) + " bytes) ";
            String title = StringUtils.rightPad(describe, 60, ".");

            getLog().info(title + " compressed at " + ratio(compressed, in) + "%");
            return in.length() - compressed.length();

        } else {
            try {
                compressed.createNewFile();
                getLog().info(in.getName() + " was zero length; not compressed.");
                return 0;
            } catch (IOException e) {
                throw new MojoExecutionException("Error handling zero length file.", e);
            }
        }
    }
    
    private long ratio(File compressed, File in) {
        long length = in.length();
        if (length == 0) {
            return 0;
        }
        return (((length - compressed.length()) * 100) / length);
    }

    class MojoErrorReporter implements ErrorReporter {

        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
            getLog().error(newMessage(message, sourceName, line, lineSource, lineOffset));
        }

        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource,
                int lineOffset) {
            error(message, sourceName, line, lineSource, lineOffset);
            throw new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
        }

        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
            getLog().warn(newMessage(message, sourceName, line, lineSource, lineOffset));
        }

        private String newMessage(String message, String sourceName, int line, String lineSource, int lineOffset) {
            StringBuffer sb = new StringBuffer();
            if (sourceName != null && sourceName.length() > 0) {
                sb.append(sourceName).append("[line ").append(line).append(", column ").append(lineOffset).append(']');
            }
            if (message != null && message.length() != 0)
                sb.append(message);
            else
                sb.append("Unknown error.");
            if (lineSource != null && lineSource.length() != 0)
                sb.append("\n\t").append(lineSource);
            return sb.toString();
        }
    }
}
