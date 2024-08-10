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
    UNKNOWN
  }

  public enum SubjectStatus
  {
    ACTIVE,
    DISABLED,
    PENDING_VALIDATION
  }


  /**
   * Returns the subject ID.
   * @return
   */
  T getSubjectID();
}
