/**
 * Copyright (C) Gustav Karlsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.kagkarlsson.scheduler.task;

public class ComposableTask {

	public static RecurringTask recurringTask(String taskName, Schedule schedule, Runnable executionHandler) {
		return recurringTask(taskName, schedule, (instance, ctx) -> executionHandler.run());
	}

	public static RecurringTask recurringTask(String taskName, Schedule schedule, ExecutionHandler executionHandler) {
		return new RecurringTask(new TaskDescriptor<Void>(taskName), schedule) {
			@Override
			public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
				executionHandler.execute(taskInstance, executionContext);
			}
		};
	}

	public static OneTimeTask onetimeTask(TaskDescriptor taskDescriptor, Runnable executionHandler) {
		return onetimeTask(taskDescriptor, (instance, ctx) -> executionHandler.run());
	}
	public static OneTimeTask onetimeTask(TaskDescriptor taskDescriptor, ExecutionHandler executionHandler) {
		return new OneTimeTask(taskDescriptor) {
			@Override
			public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
				executionHandler.execute(taskInstance, executionContext);
			}
		};
	}

	public static Task customTask(TaskDescriptor taskDescriptor, CompletionHandler completionHandler, Runnable executionHandler) {
		return customTask(taskDescriptor, completionHandler, (instance, ctx)->executionHandler.run());
	}
	public static Task customTask(TaskDescriptor taskDescriptor, CompletionHandler completionHandler, ExecutionHandler executionHandler) {
		return new Task(taskDescriptor, completionHandler, new DeadExecutionHandler.RescheduleDeadExecution()) {
			@Override
			public void execute(TaskInstance taskInstance, ExecutionContext executionContext) {
				executionHandler.execute(taskInstance, executionContext);
			}
		};
	}
}
