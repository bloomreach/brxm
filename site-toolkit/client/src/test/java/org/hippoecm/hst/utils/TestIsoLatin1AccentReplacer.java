package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.net.URLEncoder;

import org.junit.Test;

public class TestIsoLatin1AccentReplacer {
    
    @Test
    public void testUnAccentedInput() throws Exception {
        
        
        String input = "aaabbbcccdddee";
        String output = EncodingUtils.isoLatin1AccentReplacer(input);
        assertEquals("output is expected to be same as input but was not" , input, output);
        
    }
    
    @Test
    public void testAccentedInput() throws Exception {
        
        // iso latin 1 accented string
        String input = "\u00EB\u00E7\u00E9\u00DF";
        // the encoded version of the input is different
        String utf8EncodedInput = URLEncoder.encode(input, "UTF-8");
        assertNotSame("utf8EncodedInput is expected to be different then input because of accents", utf8EncodedInput ,input);
        
        String output = EncodingUtils.isoLatin1AccentReplacer(input);
        // if all accented chars are replaced correctly, then the utf-8 encoded version of the output is the same as the output
        String utf8EncodedOutput = URLEncoder.encode(output, "UTF-8");
        assertEquals("output is expected to have no accents but was '"+output+"'", utf8EncodedOutput, output);
        
    }
    
    /**
     * Now we check some chinese chars
     * @throws Exception
     */
    @Test
    public void testNonLatinInput() throws Exception {
        
        // two chinese chars
        String input = "\u53F0\u5317";
        String output = EncodingUtils.isoLatin1AccentReplacer(input);
        
        // the iso latin 1 accent replacer cannot replace chinese chars 
        assertEquals("output is expected to be same as input because cannot replace chinese chars", input , output);
        String utf8EncodedOutput = URLEncoder.encode(output, "UTF-8");
        assertNotSame("Encoded output is expect to be different because of chinese chars ", utf8EncodedOutput, output);
        
    }

}
