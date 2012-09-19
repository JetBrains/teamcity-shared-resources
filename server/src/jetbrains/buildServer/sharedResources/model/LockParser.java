package jetbrains.buildServer.sharedResources.model;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * Date: 17.09.12
 * Time: 15:16
 *
 * @author Oleg Rybak
 */
public final class LockParser {

  private static final String SIMPLE_LOCK_REGEX = "^(\\w+)$";
  private static final String RW_LOCK_REGEX = "^(\\w+)[\\s&&[^\\n]]*\\([\\s&&[^\\n]]*((read)|(write))[\\s&&[^\\n]]*\\)$";

  private static final Pattern SIMPLE_LOCK_PATTERN;
  private static final Pattern RW_LOCK_PATTERN;
  static {
    SIMPLE_LOCK_PATTERN = Pattern.compile(SIMPLE_LOCK_REGEX);
    RW_LOCK_PATTERN = Pattern.compile(RW_LOCK_REGEX);
  }

  public static Lock parse(@NotNull String input) {
    Lock result = null;
    boolean done = false;

    {
      Matcher m = RW_LOCK_PATTERN.matcher(input);
      if (m.matches()) {
        done = true;
        String name = m.group(1);
        LockType type = LockType.fromString(m.group(2));
        if (type != null) {
          result = new LockImpl(name, type);
        }
      }
    }

    if (!done) {
      Matcher m = SIMPLE_LOCK_PATTERN.matcher(input);
      if (m.matches()) {
        //done = true;
        String name = m.group(1);
        result = new LockImpl(name, LockType.SIMPLE);
      }
    }
    return result;
  }

}
