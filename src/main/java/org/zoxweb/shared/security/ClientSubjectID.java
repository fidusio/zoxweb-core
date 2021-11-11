package org.zoxweb.shared.security;

import org.zoxweb.shared.util.BaseSubjectID;

public interface ClientSubjectID
  extends BaseSubjectID<String>
{

  /**
   * @return the realm that subject belongs to
   */
  String getRealm();

  /**
   * @return subject assigned roles with in the realm
   */
  String[] getRoles();

  /**
   * @return subject assigned permissions with in the realm
   */
  String[] getPermissions();

  /**
   * @return other principals ID associated with the subject ID, email or userid or both
   */
  String[] getPrincipalIDS();
}
