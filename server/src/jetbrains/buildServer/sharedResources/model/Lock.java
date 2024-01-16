

package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

/**
 * Named lock implementation
 *
 * @author Oleg Rybak
 */
public class Lock {

  @NotNull
  private static final String NO_VALUE = "";

  /**
   * Name of the lock
   */
  @NotNull
  private final String myName;

  /**
   * Type of the lock
   */
  @NotNull
  private final LockType myType;

  /**
   * Value of the lock. Represents instance of shared resource locked
   */
  @NotNull
  private final String myValue;

  public Lock(@NotNull final String name, @NotNull final LockType type, @NotNull final String value) {
    myName = name;
    myType = type;
    myValue = value;
  }

  public Lock(@NotNull final String name, @NotNull final LockType type) {
    this(name, type, NO_VALUE);
  }


  /**
   * Creates copy of lock with given value
   *
   * @param from lock definition
   * @param value taken value
   * @return copy of combined lock definition and custom value
   */
  public static Lock createFrom(@NotNull final Lock from, @NotNull final String value) {
    return new Lock(from.getName(), from.getType(), value);
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public LockType getType() {
    return myType;
  }

  @NotNull
  public String getValue() {
    return myValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Lock)) return false;
    Lock lock = (Lock) o;
    return myName.equals(lock.myName)
            && myType == lock.myType
            && myValue.equals(lock.myValue);

  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myType.hashCode();
    result = 31 * result + myValue.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Lock{" +
            "myName='" + myName + '\'' +
            ", myType=" + myType +
            ", myValue='" + myValue + '\'' +
            '}';
  }
}