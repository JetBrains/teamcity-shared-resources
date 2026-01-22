

package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Lock type
 *
 * @author Oleg Rybak
 */
public enum LockType {

  /**
   * Shared lock
   */
  READ("readLock"),

  /**
   * Exclusive lock
   */
  WRITE("writeLock");

  @NotNull
  private final String name;

  LockType(@NotNull final String name) {
    this.name = name;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public static LockType byName(@NotNull final String name) {
    if ("readLock".equals(name)) {
      return READ;
    } else if ("writeLock".equals(name)) {
      return WRITE;
    } else {
      return null;
    }
  }

  @NotNull
  @Override
  public String toString() {
    return name;
  }

  @NotNull
  public String getDescriptiveName() {
    if (this == READ) {
      return "Read lock";
    } else {
      return "Write lock";
    }
  }
}