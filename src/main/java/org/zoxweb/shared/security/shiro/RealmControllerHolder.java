package org.zoxweb.shared.security.shiro;

public interface RealmControllerHolder<O,I> {
    RealmController<O,I> getRealmController();
    void setRealmController(RealmController<O,I> realmController);
}
