/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.server.runtime;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor (testForClass = {LocksStorage.class, LocksStorageImpl.class})
public class LocksStorageImplTest extends BaseTestCase {

  private static final String file_noValues = "lock1\treadLock\t \nlock2\twriteLock\t \n";
  private static final String file_Values = "lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\n";
  private static final String file_Mixed = "lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\nlock3\twriteLock\t ";
  private static final String file_Incorrect = "lock1\treadLock\t \nHELLO!\n";

  private LocksStorage myLocksStorage;

  private SBuild myBuild;

  private Mockery m ;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBuild = m.mock(SBuild.class);
    myLocksStorage = new LocksStorageImpl();
  }

  /**
   * Creates temp file with specified content.
   * @param content content to write
   * @return root of the created file hierarchy
   */
  private File crateTempFileWithContent(@NotNull final String content) throws Exception {
    final File artifactsDir = createTempDir();
    final File parentDir = new File(artifactsDir, LocksStorageImpl.FILE_PARENT);
    FileUtil.createDir(parentDir);
    registerAsTempFile(parentDir);
    final File artifactFile = new File(artifactsDir, LocksStorageImpl.FILE_PATH);
    FileUtil.createTempFile(parentDir, "taken_locks", "txt", true);
    registerAsTempFile(artifactFile);
    FileUtil.writeFile(artifactFile, content, "UTF-8");
    return artifactsDir;
  }

  @Test
  public void testLoad_NoValues() throws Exception {
    final File artifactsDir = crateTempFileWithContent(file_noValues);
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  public void testLoad_Values() throws Exception {
    final File artifactsDir = crateTempFileWithContent(file_Values);
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  public void testLoad_Mixed() throws Exception {
    final File artifactsDir = crateTempFileWithContent(file_Mixed);
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(3, result.size());
  }

  @Test
  public void testLoad_IgnoreIncorrectFormat() throws Exception {
    final File artifactsDir = crateTempFileWithContent(file_Incorrect);
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(1, result.size());
  }


  @Test
  public void testStore_Empty() throws Exception {
    final File artifactsDir = createTempDir();

    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<Lock, String> takenLocks = new HashMap<Lock, String>();
    myLocksStorage.store(myBuild, takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testStore_NoValues() throws Exception {
    final File artifactsDir = createTempDir();
    m.checking(new Expectations() {{
      atMost(2).of(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<Lock, String> takenLocks = new HashMap<Lock, String>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);

    takenLocks.put(lock1, "");
    takenLocks.put(lock2, "");
    myLocksStorage.store(myBuild, takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(2, result.size());
    assertContains(result.values(), lock1);
    assertContains(result.values(), lock2);
    assertEquals("", result.get(lock1.getName()).getValue());
    assertEquals("", result.get(lock2.getName()).getValue());
  }

  @Test
  public void testStore_Values() throws Exception {
    final File artifactsDir = createTempDir();
    m.checking(new Expectations() {{
      atMost(2).of(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<Lock, String> takenLocks = new HashMap<Lock, String>();
    final String value1 = "_value_1_";
    final String value2 = "_value_2_";
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    takenLocks.put(lock1, value1);
    takenLocks.put(lock2, value2);

    myLocksStorage.store(myBuild, takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(value1, result.get(lock1.getName()).getValue());
    assertEquals(value2, result.get(lock2.getName()).getValue());
  }

  @Test
  public void testStore_Mixed() throws Exception {
    final File artifactsDir = createTempDir();
    m.checking(new Expectations() {{
      atMost(2).of(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<Lock, String> takenLocks = new HashMap<Lock, String>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    final Lock lock11 = new Lock("lock11", LockType.READ);

    final String value = "_MY_VALUE_";
    takenLocks.put(lock1, "");
    takenLocks.put(lock11, value);
    takenLocks.put(lock2, "");
    myLocksStorage.store(myBuild, takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals("", result.get(lock1.getName()).getValue());
    assertEquals("", result.get(lock2.getName()).getValue());
    assertEquals(value, result.get(lock11.getName()).getValue());
  }

  @Test
  public void testArtifactExists_Yes() throws Exception {
    final File artifactsDir = crateTempFileWithContent("");
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});
    assertTrue(myLocksStorage.locksStored(myBuild));
  }


  @Test
  public void testArtifactExists_No() throws Exception {
    final File artifactsDir = createTempDir();
    m.checking(new Expectations() {{
      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
  }
}
