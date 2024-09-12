package org.zoxweb.shared.security.shiro;

public interface ShiroRealmControllerHolder<O,I> {
    ShiroRealmController<O,I> getShiroRealmController();
    void setShiroRealmController(ShiroRealmController<O,I> realmController);
}
