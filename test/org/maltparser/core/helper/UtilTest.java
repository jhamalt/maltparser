package org.maltparser.core.helper;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest {
    @Test
    public void testXmlEscape() {
        assertTrue(Util.xmlEscape("&<>\"\'").equals("&amp;&lt;&gt;&quot;&apos;"));
    }

}
