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

}
