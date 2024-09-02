package org.zoxweb.shared.util;

public interface BaseSubjectID<T>
{
  public enum SubjectType
  {
    PHONE,
    TABLET,
    USER,
    DOMAIN,
    DEVICE,
    GATEWAY,
    ENTITY,
    UNKNOWN
  }

  /**
   * Returns the subject ID.
   * @return
   */
  T getSubjectID();
}
