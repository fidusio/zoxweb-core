package org.zoxweb.shared.util;

public interface BaseSubjectID<T>
{
  enum SubjectType
  {
    PHONE,
    TABLET,
    USER,
    DOMAIN,
    DEVICE,
    GATEWAY,
    UNKNOWN
  }
  /**
   * Returns the subject ID.
   * @return
   */
  T getSubjectID();
}
