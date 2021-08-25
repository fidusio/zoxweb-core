package org.zoxweb.shared.util;

@SuppressWarnings("serial")
public class NVNumber
    extends NVBase<Number>
{
    public NVNumber()
    {
    }

    public NVNumber(String name, Number value)
    {
        super(name, value);
    }
    public NVNumber(GetName name, Number value)
    {
        super(name, value);
    }
}
