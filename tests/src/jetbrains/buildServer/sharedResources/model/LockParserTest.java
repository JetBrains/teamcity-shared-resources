package jetbrains.buildServer.sharedResources.model;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.TestFor;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 *
 * @author Oleg Rybak
 */
@TestFor (testForClass = LockParser.class)
public class LockParserTest extends BaseTestCase {

  @Test
  @TestFor (testForClass = {NamedLock.class, LockParser.class})
  public void testParseNamedLock() throws Exception {
    String[] validStrings = {
            "name",
            "another_name",
            "name1",
            "name_2",
            "123_name_321"
    };

    String[] invalidStrings = {
            " name", // leading space
            "name ", // trailing space
            "name 123", // space inside
            "name\n" // new line symbol
    };

    for(String str: validStrings) {
      Lock lock = LockParser.parse(str);
      assertNotNull(lock);
      assertTrue(lock instanceof NamedLock);
      assertEquals(str, ((NamedLock)lock).getName());
    }

    for (String str: invalidStrings) {
      assertNull(LockParser.parse(str));
    }
  }

  @Test
  @TestFor (testForClass = {ReadWriteLock.class, LockParser.class})
  public void testParseReadWriteLock() {
    final String lockName = "name";
    String[] validStringsRead = {
            "name (read)",
            "name(read)",
            "name    (read)",
            "name\t(read)",
            "name( read )",
            "name(  read  )"
    };

    String[] validStringsWrite = {
            "name (write)",
            "name(write)",
            "name    (write)",
            "name\t(write)",
            "name( write )",
            "name(  write  )"
    };

    Map<ReadWriteLock.LOCK_TYPE, List<String>> validData = new HashMap<ReadWriteLock.LOCK_TYPE, List<String>>();
    validData.put(ReadWriteLock.LOCK_TYPE.READ, Arrays.asList(validStringsRead));
    validData.put(ReadWriteLock.LOCK_TYPE.WRITE, Arrays.asList(validStringsWrite));


    for (ReadWriteLock.LOCK_TYPE type: validData.keySet()) {
      for (String str: validData.get(type)) {
        Lock lock = LockParser.parse(str);
        assertNotNull(lock);
        assertTrue(lock instanceof ReadWriteLock);
        assertEquals(type, ((ReadWriteLock)lock).getLockType());
        assertEquals(lockName, ((ReadWriteLock)lock).getName());
      }
    }

    String[] invalidStrings = {
            "name\n(read)",
            "name\n(write)",
            "name (\nread)",
            "name (read\n)",
            "name (read) ",
            "name(read)\n",
            "name(something)",
            "name( something )",
            "name(some thing)",
            "my name (is lock)"
    };


    for (String str: invalidStrings) {
      assertNull(LockParser.parse(str));
    }

    ReadWriteLock.LOCK_TYPE type = ReadWriteLock.LOCK_TYPE.fromString("some invalid type");
    assertNull(type);
  }
}
