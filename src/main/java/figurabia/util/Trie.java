/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 7, 2012
 */
package figurabia.util;

import java.util.ArrayList;
import java.util.List;

public class Trie<V> implements PrefixMap<V> {

    private List<String> keyPrefixes;
    private List<Trie<V>> subTries;
    private V value;

    @Override
    public V get(String key) {
        if (key.equals(""))
            return value;

        // in case there are no sub tries
        if (keyPrefixes == null)
            return null;

        // find appropriate sub trie and recurse
        for (int i = 0; i < keyPrefixes.size(); i++) {
            String prefix = keyPrefixes.get(i);
            if (key.startsWith(prefix))
                return subTries.get(i).get(key.substring(prefix.length()));
        }

        // return null if not found
        return null;
    }

    @Override
    public void put(String key, V value) {
        if (key.equals("")) {
            this.value = value;
        } else if (keyPrefixes == null) {
            keyPrefixes = new ArrayList<String>();
            keyPrefixes.add(key);
            subTries = new ArrayList<Trie<V>>();
            Trie<V> t = new Trie<V>();
            t.put("", value);
            subTries.add(t);
        } else {

            // see if there is an existing sub trie
            char firstChar = key.charAt(0);
            boolean matched = false;
            for (int i = 0; i < keyPrefixes.size(); i++) {
                String prefix = keyPrefixes.get(i);
                if (prefix.charAt(0) == firstChar) {
                    matched = true;
                    if (key.startsWith(prefix)) {
                        // whole prefix is a part of the key -> recurse
                        subTries.get(i).put(key.substring(prefix.length()), value);
                    } else {
                        // only part of the prefix is part of the key -> split existing prefix
                        String commonPrefix = findCommonPrefix(key, prefix);

                        // split into two
                        Trie<V> formerTrie = subTries.get(i);
                        Trie<V> splitTrie = new Trie<V>();
                        keyPrefixes.set(i, commonPrefix);
                        subTries.set(i, splitTrie);
                        splitTrie.keyPrefixes = new ArrayList<String>();
                        splitTrie.subTries = new ArrayList<Trie<V>>();
                        splitTrie.keyPrefixes.add(prefix.substring(commonPrefix.length()));
                        splitTrie.subTries.add(formerTrie);
                        splitTrie.put(key.substring(commonPrefix.length()), value);
                    }
                    break;
                }
            }
            if (!matched) {
                // add new entry if there was none
                int insertPos = keyPrefixes.size();
                for (int i = 0; i < keyPrefixes.size(); i++) {
                    if (firstChar > keyPrefixes.get(i).charAt(0)) {
                        insertPos = i;
                        break;
                    }
                }
                Trie<V> t = new Trie<V>();
                t.put("", value);
                keyPrefixes.add(insertPos, key);
                subTries.add(insertPos, t);
            }
        }
    }

    private String findCommonPrefix(String a, String b) {
        for (int i = 0; i < a.length() && i < b.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return a.substring(0, i);
        }
        return a.substring(0, Math.min(a.length(), b.length()));
    }

    @Override
    public List<V> valuesAtPrefixOf(String s) {
        List<V> list = new ArrayList<V>();

        addPrefixValuesToList(this, s, list);
        return list;
    }

    private static <V> void addPrefixValuesToList(Trie<V> t, String s, List<V> list) {
        if (t.keyPrefixes != null) {
            for (int i = 0; i < t.keyPrefixes.size(); i++) {
                String prefix = t.keyPrefixes.get(i);
                if (s.startsWith(prefix)) {
                    addPrefixValuesToList(t.subTries.get(i), s.substring(prefix.length()), list);
                    break;
                }
            }
        }
        if (t.value != null)
            list.add(t.value);
    }

    @Override
    public V remove(String key) {
        if (key.equals("")) {
            V v = value;
            value = null;
            return v;
        }
        if (keyPrefixes != null) {

            for (int i = 0; i < keyPrefixes.size(); i++) {
                String prefix = keyPrefixes.get(i);
                Trie<V> subTrie = subTries.get(i);
                if (key.startsWith(prefix)) {
                    if (key.length() == prefix.length()) {
                        // found exact place to remove
                        // only remove, if it has no sub tries
                        V v = subTrie.remove("");
                        if (subTrie.keyPrefixes == null || subTrie.keyPrefixes.size() == 0) {
                            keyPrefixes.remove(i);
                            subTries.remove(i);
                        }
                        return v;
                    } else {
                        // recurse
                        V value = subTries.get(i).remove(key.substring(prefix.length()));
                        // check if sub trie can be merged with this one
                        if (subTrie.keyPrefixes != null && subTrie.keyPrefixes.size() == 1 && subTrie.value == null) {
                            keyPrefixes.set(i, prefix + subTrie.keyPrefixes.get(0));
                            subTries.set(i, subTrie.subTries.get(0));
                        }
                        return value;
                    }
                }
            }
        }
        return null;
    }
}
