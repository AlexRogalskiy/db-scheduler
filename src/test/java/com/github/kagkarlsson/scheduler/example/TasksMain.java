package com.github.kagkarlsson.scheduler.example;

import com.github.kagkarlsson.scheduler.HsqlTestDatabaseRule;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.ofHours;

public class TasksMain {
	private static final Logger LOG = LoggerFactory.getLogger(TasksMain.class);

	public static void main(String[] args) throws Throwable {
		try {
			final HsqlTestDatabaseRule hsqlRule = new HsqlTestDatabaseRule();
			hsqlRule.before();
			final DataSource dataSource = hsqlRule.getDataSource();

//			recurringTask(dataSource);
//			adhocExecution(dataSource);
			adhocExecutionWithData(dataSource);
//			simplerTaskDefinition(dataSource);
		} catch (Exception e) {
			LOG.error("Error", e);
		}
	}

	private static void recurringTask(DataSource dataSource) {

		final MyHourlyTask hourlyTask = new MyHourlyTask();

		final Scheduler scheduler = Scheduler
				.create(dataSource)
				.startTasks(hourlyTask)
				.threads(5)
				.build();

		// hourlyTask is automatically scheduled on startup if not already started (i.e. in the db)
		scheduler.start();
	}

	public static class MyHourlyTask extends RecurringTask {

		public MyHourlyTask() {
			super("my-hourly-task", FixedDelay.of(ofHours(1)));
		}

		@Override
		public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
			System.out.println("Executed!");
		}
	}

	private static void adhocExecution(DataSource dataSource) {

		final MyAdhocTask myAdhocTask = new MyAdhocTask();

		final Scheduler scheduler = Scheduler
				.create(dataSource, myAdhocTask)
				.threads(5)
				.build();

		scheduler.start();

		// Schedule the task for execution a certain time in the future
		scheduler.scheduleForExecution(Instant.now().plusSeconds(5), myAdhocTask.instance("1045"));
	}

	public static class MyAdhocTask extends OneTimeTask {

		public MyAdhocTask() {
			super("my-adhoc-task");
		}

		@Override
		public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
			System.out.println("Executed!");
		}
	}

	private static void adhocExecutionWithData(DataSource dataSource) {

		final MyAdhocTaskWithData myAdhocTask = new MyAdhocTaskWithData();

		final Scheduler scheduler = Scheduler
				.create(dataSource, myAdhocTask)
				.threads(5)
				.build();

		scheduler.start();

		// Schedule the task for execution a certain time in the future
		scheduler.scheduleForExecution(Instant.now().plusSeconds(5), myAdhocTask.instance("1045", new MyData(1001L)));
	}

	public static class MyData implements Serializable {
		public final long primaryKey;

		public MyData(long primaryKey) {
			this.primaryKey = primaryKey;
		}
	}

	public static class MyAdhocTaskWithData extends OneTimeTaskWithData<MyData> {

		public MyAdhocTaskWithData() {
			super("my-adhoc-task-with-data", new JavaSerializer<>());
		}

		@Override
		public void execute(TaskInstance taskInstance, MyData taskData, ExecutionContext executionContext) {
			System.out.println("Executed with data: primaryKey=" + taskData.primaryKey);
		}
	}


	private static void simplerTaskDefinition(DataSource dataSource) {

		final RecurringTask myHourlyTask = ComposableTask.recurringTask("my-hourly-task", FixedDelay.of(ofHours(1)),
						() -> System.out.println("Executed!"));


		final Scheduler scheduler = Scheduler
				.create(dataSource)
				.startTasks(myHourlyTask)
				.threads(5)
				.build();

		scheduler.start();

		final OneTimeTask oneTimeTask = ComposableTask.onetimeTask("my-onetime-task",
				(taskInstance, context) -> System.out.println("One-time with id "+taskInstance.getId()+" executed!"));

		scheduler.scheduleForExecution(Instant.now().plus(Duration.ofDays(1)), oneTimeTask.instance("1001"));
	}


	private static void springWorkerExample(DataSource dataSource, MySpringWorker mySpringWorker) {

		// instantiate and start the scheduler somewhere in your application
		final Scheduler scheduler = Scheduler
				.create(dataSource)
				.threads(2)
				.build();
		scheduler.start();

		// define a task and a handler that named task, MySpringWorker implements the ExecutionHandler interface
		final OneTimeTask oneTimeTask = ComposableTask.onetimeTask("my-onetime-task", mySpringWorker);

		// schedule a future execution for the task with a custom id (currently the only form for context supported)
		scheduler.scheduleForExecution(Instant.now().plus(Duration.ofDays(1)), oneTimeTask.instance("1001"));
	}


	public static class MySpringWorker implements ExecutionHandler {
		public MySpringWorker() {
			// could be instantiated by Spring
		}

		@Override
		public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
			System.out.println("Executed task with id="+taskInstance.getId());
		}
	}


	public static class MyExecutionHandler implements ExecutionHandler {

		@Override
		public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
			System.out.println("Executed!");
		}
	}
}
