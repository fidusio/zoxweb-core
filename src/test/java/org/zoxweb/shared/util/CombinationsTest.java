package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

public class CombinationsTest {

    @Test
    public void createCombosList()
    {
        Object[] array = {"a", String.class, "c", "d"};
        System.out.println( SharedUtil.combinationsAsList(true, array));
        System.out.println( SharedUtil.combinationsAsList(false, array));
    }
    @Test
    public void createCombosSet()
    {
        Object[] array = {"a", String.class, "c", "d"};
        Set<Set<Object>> combo = SharedUtil.combinationsAsSet(true, array);
        System.out.println( SharedUtil.combinationsAsSet(true, array));
        System.out.println( SharedUtil.combinationsAsSet(false, array));
    }
}
