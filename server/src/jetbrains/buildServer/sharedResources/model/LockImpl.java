package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Typed lock implementation
 *
 * @author Oleg Rybak
 */
public class LockImpl implements Lock {

  private final String myName;

  private final LockType myType;

  public LockImpl(@NotNull String name, @NotNull LockType type) {
    myName = name;
    myType = type;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public LockType getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LockImpl)) return false;

    LockImpl lock = (LockImpl) o;

    return myName.equals(lock.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
