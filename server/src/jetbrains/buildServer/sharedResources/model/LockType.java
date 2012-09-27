package jetbrains.buildServer.sharedResources.model;

/**
 * Created with IntelliJ IDEA.
 * Date: 19.09.12
 * Time: 13:27
 *
 * @author Oleg Rybak
 */
public enum LockType {
  READ("readLock"),
  WRITE("writeLock");

  private final String name;

  private LockType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
