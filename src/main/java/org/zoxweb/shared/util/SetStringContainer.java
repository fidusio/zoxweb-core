package org.zoxweb.shared.util;

public class SetStringContainer
    extends SetContainer<String>

{

    private final Const.StringType type;

    public SetStringContainer(Const.StringType type)
    {
        this.type = type != null ? type : Const.StringType.AS_IS;
    }

    private String convert(String str)
    {
        SUS.checkIfNulls("null string", str);
        switch (type)
        {
            case UPPER:
                return str.toUpperCase();

            case LOWER:
                return str.toLowerCase();
            default:
                return str;
        }
    }
    public boolean contains(String str)
    {
        return set.contains(convert(str));
    }

    /**
     * @param s
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public String validate(String s) throws IllegalArgumentException, NullPointerException {
        s = convert(s);
        if(!contains(s))
            throw new IllegalArgumentException("Not found " + s);

        return s;
    }

    public SetStringContainer add(String str)
    {
        return super.add(convert(str));
    }

    public SetStringContainer remove(String str)
    {
        return super.remove(convert(str));
    }



}
