package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * Date: 19.09.12
 * Time: 13:27
 *
 * @author Oleg Rybak
 */
public enum LockType {
  SIMPLE,
  READ,
  WRITE;

  public static LockType fromString(@NotNull String str) {
    if ("read".equals(str)) {
      return READ;
    } else if ("write".equals(str)) {
      return WRITE;
    } else if ("".equals(str)) {
      return SIMPLE;
    }else {
      return null;
    }
  }
}
