/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import com.google.common.cache.Cache;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor (testForClass = {LocksStorage.class, LocksStorageImpl.class})
public class LocksStorageImplTest extends BaseTestCase {

  private static final String file_noValues = "lock1\treadLock\t \nlock2\twriteLock\t \nlock3\treadLock\t \nlock4\twriteLock\t \n";
  private static final String file_Values = "lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\n";
  private static final String file_Mixed = "lock1\treadLock\tMy Value 1\nlock2\twriteLock\tMy Value 2\nlock3\twriteLock\t ";
  private static final String file_Incorrect = "lock1\treadLock\t \nHELLO!\n";

  private final Long buildId = 1L;

  private LocksStorage myLocksStorage;

  private SBuild myBuild;

  private Mockery m ;

  private EventDispatcher<BuildServerListener> myDispatcher;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBuild = m.mock(SBuild.class);
    myDispatcher = EventDispatcher.create(BuildServerListener.class);
    myLocksStorage = new LocksStorageImpl(myDispatcher);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  public void testLoad_NoValues() throws Exception {
    final File artifactsDir = createTempFileWithContent(file_noValues);
    addSingleArtifactsAccessExpectations(artifactsDir);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);

    assertNotNull(result);
    assertEquals(4, result.size());
  }

  @Test
  public void testLoad_Values() throws Exception {
    final File artifactsDir = createTempFileWithContent(file_Values);
    addSingleArtifactsAccessExpectations(artifactsDir);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(2, result.size());

  }

  @Test
  public void testLoad_Mixed() throws Exception {
    final File artifactsDir = createTempFileWithContent(file_Mixed);
    addSingleArtifactsAccessExpectations(artifactsDir);

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(3, result.size());
  }

  @Test
  public void testLoad_IgnoreIncorrectFormat() throws Exception {
    final File artifactsDir = createTempFileWithContent(file_Incorrect);
    addSingleArtifactsAccessExpectations(artifactsDir);

    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testStore_Empty() throws Exception {
    final File artifactsDir = createTempDir();
    addSingleArtifactsAccessExpectations(artifactsDir);

    final Map<Lock, String> takenLocks = new HashMap<>();
    myLocksStorage.store(myBuild, takenLocks);
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testStore_NoValues() throws Exception {
    final File artifactsDir = createTempDir();
    addSingleArtifactsAccessExpectations(artifactsDir);

    final Map<Lock, String> takenLocks = new HashMap<>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);

    takenLocks.put(lock1, "");
    takenLocks.put(lock2, "");
    myLocksStorage.store(myBuild, takenLocks);
    // values are in cache. No file access needed
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
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
    addSingleArtifactsAccessExpectations(artifactsDir);
    final Map<Lock, String> takenLocks = new HashMap<>();
    final String value1 = "_value_1_";
    final String value2 = "_value_2_";
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    takenLocks.put(lock1, value1);
    takenLocks.put(lock2, value2);
    myLocksStorage.store(myBuild, takenLocks);

    // values are in cache. No file access needed
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
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
      exactly(2).of(myBuild).getBuildId();
      will(returnValue(buildId));

      atMost(2).of(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});

    final Map<Lock, String> takenLocks = new HashMap<>();
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
  public void testArtifactExist_CacheYes() throws Exception {
    // check that call of 'locksStored' method does not cause disk access
    m.checking(new Expectations() {{
      exactly(2).of(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
    assertFalse(myLocksStorage.locksStored(myBuild));

    // store locks. expect 1 disk access here
    storeSomeLocks(myBuild);

    // subsequent 'locksStored' method calls return true and do not cause disk access
    m.checking(new Expectations() {{
      exactly(2).of(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertTrue(myLocksStorage.locksStored(myBuild));
    assertTrue(myLocksStorage.locksStored(myBuild));
  }


  @Test
  @TestFor(issues = "TW-44474")
  @SuppressWarnings("unchecked")
  public void testReloadTakenLocks_EvictedCache() throws Exception {
    final File artifactsDir = createTempDir();
    // no locks
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
    // store some
    storeSomeLocks(myBuild, artifactsDir);
    // check we have them in cache
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertTrue(myLocksStorage.locksStored(myBuild));
    // evict value cache, but not exists cache
    Field cacheField = myLocksStorage.getClass().getDeclaredField("myLocksCache");
    cacheField.setAccessible(true);
    final Cache<SBuild,  Map<String, Lock>> cache = (Cache<SBuild,  Map<String, Lock>>)cacheField.get(myLocksStorage);
    cache.invalidate(myBuild);
    // call 'locksStored'. Should return true without accessing disk
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    // locksStored flag must remain
    assertTrue(myLocksStorage.locksStored(myBuild));
    // call load 2 times. Expect 1 disk access
    m.checking(new Expectations() {{
      exactly(2).of(myBuild).getBuildId();
      will(returnValue(buildId));

      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});
    final Map<String, Lock> result = myLocksStorage.load(myBuild);
    assertNotNull(result);
    assertEquals(1, result.size());
    myLocksStorage.load(myBuild);
  }

  @Test
  @TestFor(issues = "TW-44474")
  public void testBuildFinished_CacheCleaned() throws Exception {
    final File artifactsDir = createTempDir();
    // no locks
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
    // store some
    storeSomeLocks(myBuild, artifactsDir);
    // check we have them in cache
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertTrue(myLocksStorage.locksStored(myBuild));
    // terminate build
    SRunningBuild runningBuild = m.mock(SRunningBuild.class);
    m.checking(new Expectations() {{
      allowing(runningBuild).getBuildId();
      will(returnValue(buildId));
    }});
    myDispatcher.getMulticaster().buildFinished(runningBuild);
    // check that locks are no longer stored
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
  }


  @Test
  @TestFor(issues = "TW-44474")
  public void testArtifactExists_No() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));
    }});
    assertFalse(myLocksStorage.locksStored(myBuild));
  }

  @Test
  @TestFor(issues = "TW-31068")
  public void testLoadStore_MultiThreaded() throws Exception {
    final File artifactsDir = createTempDir();
    final CountDownLatch myLatch = new CountDownLatch(2);

    final Map<Lock, String> takenLocks = new HashMap<>();
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    final Lock lock11 = new Lock("lock11", LockType.READ);

    final String value = "_MY_VALUE_";
    takenLocks.put(lock1, "");
    takenLocks.put(lock11, value);
    takenLocks.put(lock2, "");

    m.checking(new Expectations() {{
      allowing(myBuild).getBuildId();
      will(returnValue(buildId));

      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});


    final Runnable runReader = () -> {
      while (!myLocksStorage.locksStored(myBuild)) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          fail(e.getMessage());
        }
      }
      final Map<String, Lock> result = myLocksStorage.load(myBuild);
      assertNotNull(result);
      assertEquals(3, result.size());
      assertEquals("", result.get(lock1.getName()).getValue());
      assertEquals("", result.get(lock2.getName()).getValue());
      assertEquals(value, result.get(lock11.getName()).getValue());
      myLatch.countDown();
    };
    new Thread(runReader).start();

    final Runnable runWriter = () -> {
      myLocksStorage.store(myBuild, takenLocks);
      myLatch.countDown();
    };
    new Thread(runWriter).start();
    myLatch.await(10, TimeUnit.SECONDS);
  }


  /**
   * Creates temp file with specified content.
   * @param content content to write
   * @return root of the created file hierarchy
   */
  private File createTempFileWithContent(@NotNull final String content) throws Exception {
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

  /**
   * Creates artifact with locks. Invokes {@code store} method, adds necessary expectations
   * @param build build to store locks for
   * @throws Exception if something goes wrong
   */
  private void storeSomeLocks(@NotNull final SBuild build) throws Exception {
    storeSomeLocks(build, createTempDir());
  }

  /**
   * Creates artifact with locks inside predefined directory. Invokes {@code store} method, adds necessary expectations
   * @param build build to store locks for
   */
  private void storeSomeLocks(@NotNull final SBuild build, @NotNull final File artifactsDir) {
    final Map<Lock, String> someLocks = new HashMap<Lock, String>() {{
      put(new Lock("someLock", LockType.READ), "SOME_VALUE");
    }};
    addSingleArtifactsAccessExpectations(artifactsDir);
    myLocksStorage.store(build, someLocks);
  }

  /**
   * Adds mock expectations for single access of artifact file inside {@code artifactsDir}
   * @param artifactsDir artifacts directory
   */
  private void addSingleArtifactsAccessExpectations(@NotNull final File artifactsDir) {
    m.checking(new Expectations() {{
      oneOf(myBuild).getBuildId();
      will(returnValue(buildId));

      oneOf(myBuild).getArtifactsDirectory();
      will(returnValue(artifactsDir));
    }});
  }
}
