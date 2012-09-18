package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;

/**
 * Class {@code ReadWriteLock}
 *
 * Stores lock type and lock name
 *
 * @author Oleg Rybak
 */
public final class ReadWriteLock extends AbstractLock implements Lock {

  private final LOCK_TYPE lockType;

  public static ReadWriteLock createReadWriteLock(@NotNull String name, @NotNull LOCK_TYPE lockType) {
    return new ReadWriteLock(name, lockType);
  }

  private ReadWriteLock(@NotNull String name, @NotNull LOCK_TYPE lockType) {
    super(name);
    this.lockType = lockType;
  }

  @NotNull
  public LOCK_TYPE getLockType() {
    return lockType;
  }

  public static enum LOCK_TYPE {
    READ(),
    WRITE();

    public static LOCK_TYPE fromString(@NotNull String str) {
      if ("read".equals(str)) {
        return READ;
      } else if ("write".equals(str)) {
        return WRITE;
      } else {
        return null;
      }
    }
  }
}
