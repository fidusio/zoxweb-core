package org.zoxweb.shared.util;

public class SetStringContainer
        extends SetContainer<String> {

    private final Const.StringType type;

    public SetStringContainer(Const.StringType type) {
        this.type = type != null ? type : Const.StringType.AS_IS;
    }


    public boolean contains(String str) {
        return set.contains(Const.StringType.convert(type, str));
    }

    /**
     * @param s to be validated
     * @return validated value
     * @throws IllegalArgumentException if there issue will s
     */
    @Override
    public String validate(String s) throws IllegalArgumentException, NullPointerException {
        s = Const.StringType.convert(type, s);
        if (!contains(s))
            throw new IllegalArgumentException("Not found " + s);

        return s;
    }

    public SetStringContainer add(String str) {
        return super.add(Const.StringType.convert(type, str));
    }

    public SetStringContainer remove(String str) {
        return super.remove(Const.StringType.convert(type, str));
    }


}
