/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.scheduling.internal.upgrade.datastore;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.Cooperation2Factory;
import org.sonatype.nexus.common.cooperation2.Cooperation2Selector;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.common.event.EventHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.upgrade.events.UpgradeCompletedEvent;
import org.sonatype.nexus.common.upgrade.events.UpgradeFailedEvent;
import org.sonatype.nexus.scheduling.ExternalTaskState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskState;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedCanceled;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedDone;
import org.sonatype.nexus.scheduling.events.TaskEventStoppedFailed;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseExtension;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QueuingUpgradeTaskScheduler}
 */
@ExtendWith(DatabaseExtension.class)
class QueuingUpgradeTaskSchedulerTest
    extends Test5Support
{
  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  private static final String TASK_ID = "my-upgrade-task";

  private static final String TASK_ID_2 = "my-upgrade-task-2";

  private static final Map<String, String> TASK_CONFIG = ImmutableMap.of("a", "b");

  @Mock
  private PeriodicJobService periodicJobService;

  @Mock
  private EventManager eventManager;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private TaskConfiguration taskConfiguration;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private Cooperation2Factory cooperationFactory;

  @Mock
  private Cooperation2Selector localCooperationSelector;

  @Mock
  private Cooperation2Selector distributedCooperationSelector;

  private QueuingUpgradeTaskScheduler underTest;

  private UpgradeTaskStore upgradeTaskStore;

  @DataSessionConfiguration(daos = UpgradeTaskDAO.class)
  TestDataSessionSupplier sessionRule;

  @BeforeEach
  public void start() {
    mockCooperation();

    lenient().when(taskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);
    lenient().when(taskConfiguration.asMap()).thenReturn(TASK_CONFIG);
    lenient().when(taskConfiguration.getId()).thenReturn(TASK_ID);

    lenient().when(taskInfo.getId()).thenReturn(TASK_ID);
    lenient().when(taskInfo.getConfiguration()).thenReturn(taskConfiguration);

    lenient().when(taskScheduler.submit(any())).thenReturn(taskInfo);

    upgradeTaskStore = new UpgradeTaskStore(sessionRule);
    upgradeTaskStore.setDependencies(eventManager);

    underTest = new QueuingUpgradeTaskScheduler(periodicJobService, taskScheduler, upgradeTaskStore, true,
        Duration.ofSeconds(0), localCooperationSelector, distributedCooperationSelector);
  }

  @AfterEach
  public void stop() {
    // the default behavior is to use the distributed impl, so we should have selected it and see nothing be done with
    // the local impl
    verifyNoInteractions(localCooperationSelector);
  }

  /*
   * Verify that tasks requested to be scheduled before startup are scheduled after startup
   */
  @DatabaseTest
  public void testSchedule_beforeStart() throws Exception {
    underTest.schedule(taskConfiguration);

    // the scheduler isn't invoked, delay due to async
    await().during(ONE_SECOND)
        .untilAsserted(() -> verify(taskScheduler, never()).submit(any()));

    // but we have a record in the DB in case something happens
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    startQueuingUpgradeTaskScheduler();

    await().atMost(ONE_SECOND).untilAsserted(() -> verify(taskScheduler).submit(any()));
  }

  /*
   * Verify that tasks requested to be scheduled after startup are scheduled immediately
   */
  @DatabaseTest
  public void testSchedule_afterStart() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    // we have a DB record
    assertThat(upgradeTaskStore.browse().count(), is(1L));
    // and the task was scheduled

    verifyNoInteractions(taskScheduler);
  }

  @DatabaseTest
  public void testOn_upgradeCompleted() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    EventHelper.asReplicating(
        () -> underTest.on(new UpgradeCompletedEvent("jsmith", "123", Collections.emptyList(), "Migration_1.0")));

    // replicating events from other nodes should not trigger
    verifyNoInteractions(taskScheduler);

    underTest.on(new UpgradeCompletedEvent("jsmith", "123", Collections.emptyList(), "Migration_1.0"));

    // non-replicated events should trigger
    verify(taskScheduler).submit(any());
  }

  @DatabaseTest
  public void testOn_upgradeFailed() throws Exception {
    startQueuingUpgradeTaskScheduler();
    underTest.schedule(taskConfiguration);

    EventHelper.asReplicating(
        () -> underTest.on(new UpgradeFailedEvent("jsmith", "123", "Failed", "Migration_1.0")));

    // replicating events from other nodes should not trigger
    verifyNoInteractions(taskScheduler);

    underTest.on(new UpgradeFailedEvent("jsmith", "123", "Failed", "Migration_1.0"));

    // non-replicated events should trigger
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing a task completed event.
   */
  @DatabaseTest
  public void testOn_taskCompleted() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedDone(taskInfo));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID_2, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(2L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedDone(taskInfo));
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // queue is processed
    verify(taskScheduler, times(2)).getTaskById(TASK_ID_2);
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue which has no associated task
   */
  @DatabaseTest
  public void testMaybeStartQueue_nextItem_hasNoTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler, times(2)).getTaskById(TASK_ID);
    verify(taskScheduler).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue and the task indicates OK
   */
  @DatabaseTest
  public void testMaybeStartQueue_nextItem_hasOkTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.OK, null, null, null, null, null));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler).getTaskById(TASK_ID);
    verify(taskScheduler, never()).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with an item in the queue and the associated task is currently running
   */
  @DatabaseTest
  public void testMaybeStartQueue_nextItem_hasRunningTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    when(taskScheduler.getTaskById(TASK_ID)).thenReturn(taskInfo);
    when(taskScheduler.toExternalTaskState(taskInfo))
        .thenReturn(new ExternalTaskState(TaskState.RUNNING, null, null, null, null, null));

    // Start queue
    underTest.maybeStartQueue();

    verify(taskScheduler).getTaskById(TASK_ID);
    verify(taskScheduler, never()).submit(any());
  }

  /*
   * Verify behaviour when processing is invoked with no items in the queue
   */
  @DatabaseTest
  public void testMaybeStartQueue_noQueuedTask() throws Exception {
    startQueuingUpgradeTaskScheduler();

    // Start queue
    underTest.maybeStartQueue();

    verifyNoInteractions(taskScheduler);
  }

  /*
   * Verify behaviour when processing a task canceled event.
   */
  @DatabaseTest
  public void testOn_taskCanceled() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedCanceled(taskInfo));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedCanceled(taskInfo));
    UpgradeTaskData task = upgradeTaskStore.browse().findFirst().orElse(null);
    assertThat(task, notNullValue());
    assertThat(task.getStatus(), is("canceled"));
  }

  /*
   * Verify behaviour when processing a task failed event.
   */
  @DatabaseTest
  public void testOn_taskFailed() throws Exception {
    startQueuingUpgradeTaskScheduler();
    // No task has been registered as an upgrade, the main thing here is that we don't fail
    underTest.on(new TaskEventStoppedFailed(taskInfo, null));
    // we don't expect anything to have been added to the DB
    assertThat(upgradeTaskStore.browse().count(), is(0L));

    // Add a record about an upgrade task to the DB
    upgradeTaskStore.insert(new UpgradeTaskData(TASK_ID, TASK_CONFIG));
    // sanity check
    assertThat(upgradeTaskStore.browse().count(), is(1L));

    // Fire event about the task in the DB
    underTest.on(new TaskEventStoppedFailed(taskInfo, null));
    UpgradeTaskData task = upgradeTaskStore.browse().findFirst().orElse(null);
    assertThat(task, notNullValue());
    assertThat(task.getStatus(), is("failed"));
  }

  private void mockCooperation() {
    lenient().when(localCooperationSelector.select()).thenReturn(cooperationFactory);
    when(distributedCooperationSelector.select()).thenReturn(cooperationFactory);

    Cooperation2Factory.Builder cooperationBuilder = mock(Cooperation2Factory.Builder.class);
    when(cooperationFactory.configure()).thenReturn(cooperationBuilder);

    Cooperation2 disabled = new DefaultCooperation2Factory().configure()
        .enabled(false)
        .build("test");

    when(cooperationBuilder.build(any())).thenReturn(disabled);
  }

  /*
   * Trigger stateguard transition and wait for async behaviour
   */
  private void startQueuingUpgradeTaskScheduler() throws Exception {
    ForkJoinTask<?>[] startup = new ForkJoinTask[1];

    doAnswer(i -> {
      // The runnable passed in must only be executed after StateGuard finishes the START transition, so we need async
      startup[0] = ForkJoinTask.adapt(() -> {
        // Wait for stateguard to finish the transition
        await().atMost(1, TimeUnit.SECONDS).until(underTest::isStarted);
        // invoke the job
        ((Runnable) i.getArguments()[0]).run();
        // dummy return value
        return null;
      });

      // invoke the async
      ForkJoinPool.commonPool().execute(startup[0]);

      return null;
    }).when(periodicJobService).runOnce(any(), anyInt());

    underTest.start();

    // wait for the async task to complete
    startup[0].get();
  }
}
