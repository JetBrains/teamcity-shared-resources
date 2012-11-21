package jetbrains.buildServer.sharedResources.model;

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

  private final String name;

  private LockType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static LockType byName(String name) {
    if ("readLock".equals(name)) {
      return READ;
    } else if ("writeLock".equals(name)) {
      return WRITE;
    } else {
      return null;
    }
  }

  @Override
  public String toString() {
    return name;
  }
}
