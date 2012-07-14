/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 8, 2012
 */
package figurabia.util;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;

public class TrieTest {

    @Test
    public void testPutGet() {
        Trie<Integer> t = new Trie<Integer>();

        t.put("a", 1);
        t.put("c", 3);
        t.put("b", 2);
        t.put("ab", 12);
        t.put("ba", 21);
        t.put("ca", 31);
        t.put("", 0);

        Assert.assertEquals((Integer) 2, t.get("b"));
        Assert.assertEquals((Integer) 12, t.get("ab"));
        Assert.assertEquals((Integer) 3, t.get("c"));
        Assert.assertEquals((Integer) 31, t.get("ca"));
        Assert.assertEquals((Integer) 0, t.get(""));
    }

    @Test
    public void testPutRemove() {
        Trie<Integer> t = new Trie<Integer>();

        Assert.assertNull(t.get("abc"));
        t.put("abc", 5);
        Assert.assertEquals((Integer) 5, t.get("abc"));
        Assert.assertEquals((Integer) 5, t.remove("abc"));
        Assert.assertNull(t.get("abc"));
        t.put("abc", 5);
        Assert.assertEquals((Integer) 5, t.get("abc"));

        Assert.assertEquals((Integer) 5, t.remove("abc"));
        Assert.assertNull(t.get("abc"));

        Assert.assertNull(t.remove("abc"));
        Assert.assertNull(t.get("abc"));

        Assert.assertNull(t.remove("xyz"));

        t.put("abc", 5);
        t.put("abcde", 6);
        t.put("abcdef", 7);
        t.put("abcdex", 8);
        t.put("", 2);

        Assert.assertEquals((Integer) 6, t.get("abcde"));
        Assert.assertEquals((Integer) 6, t.remove("abcde"));
        Assert.assertNull(t.get("abcde"));

        Assert.assertEquals((Integer) 5, t.get("abc"));
        Assert.assertEquals((Integer) 5, t.remove("abc"));
        Assert.assertNull(t.get("abc"));

        Assert.assertEquals((Integer) 8, t.get("abcdex"));
        Assert.assertEquals((Integer) 8, t.remove("abcdex"));
        Assert.assertNull(t.get("abcdex"));

        Assert.assertEquals((Integer) 2, t.get(""));
        Assert.assertEquals((Integer) 2, t.remove(""));
        Assert.assertNull(t.get(""));

        Assert.assertEquals((Integer) 7, t.get("abcdef"));
        Assert.assertEquals((Integer) 7, t.remove("abcdef"));
        Assert.assertNull(t.get("abcdef"));
    }

    @Test
    public void testPrefix() {
        Trie<Integer> t = new Trie<Integer>();

        Assert.assertEquals(Collections.emptyList(), t.valuesAtPrefixOf(""));
        Assert.assertEquals(Collections.emptyList(), t.valuesAtPrefixOf("abc"));

        t.put("abc", 25);
        t.put("ab", 26);
        t.put("abd", 28);

        Assert.assertEquals(Collections.emptyList(), t.valuesAtPrefixOf(""));
        Assert.assertEquals(Arrays.asList(25, 26), t.valuesAtPrefixOf("abc"));

        t.put("", -15);

        Assert.assertEquals(Arrays.asList(-15), t.valuesAtPrefixOf(""));
        Assert.assertEquals(Arrays.asList(25, 26, -15), t.valuesAtPrefixOf("abc"));

    }
}
