package org.zoxweb;

//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.AddressInfo;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.NVEntity;

public class ClassInstance {

    @Test
    public void testAssignableFrom()
    {
        assert(Enum.class.isAssignableFrom(Const.TimeInMillis.class));
        assert(NVEntity.class.isAssignableFrom(AddressInfo.class));
    }

    @Test
    public void testInstanceOf()
    {
        assert(new AddressInfo() instanceof NVEntity);
    }


    @Test
    public void nullInstanceOf()
    {
        Object tst = "jkjksd";
        assert(tst instanceof CharSequence);
        tst = null;
        assert(!(tst instanceof CharSequence));


    }

}
