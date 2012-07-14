/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 7, 2012
 */
package figurabia.util;

import java.util.List;

public interface PrefixMap<V> {

    List<V> valuesAtPrefixOf(String s);

    V get(String key);

    void put(String key, V value);

    V remove(String key);
}
