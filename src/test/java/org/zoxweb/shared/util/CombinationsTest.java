package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

public class CombinationsTest {

    @Test
    public void createCombosList()
    {
        Object[] array = {"a", String.class, "c", "d"};
        System.out.println( SUS.combinationsAsList(true, array));
        System.out.println( SUS.combinationsAsList(false, array));
    }
    @Test
    public void createCombosSet()
    {
        Object[] array = {"a", String.class, "c", "d"};
        Set<Set<Object>> combo = SUS.combinationsAsSet(true, array);
        System.out.println( SUS.combinationsAsSet(true, array));
        System.out.println( SUS.combinationsAsSet(false, array));
    }
}
