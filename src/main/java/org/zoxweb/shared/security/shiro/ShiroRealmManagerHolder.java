package org.zoxweb.shared.security.shiro;

public interface ShiroRealmManagerHolder<O,I> {
    ShiroRealmManager<O,I> getShiroRealmManger();
    void setShiroRealmManagerHolder();
}
