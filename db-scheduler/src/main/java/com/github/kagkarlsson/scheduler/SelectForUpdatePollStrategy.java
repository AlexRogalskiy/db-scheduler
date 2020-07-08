package com.github.kagkarlsson.scheduler;

import com.github.kagkarlsson.scheduler.stats.StatsRegistry;
import com.github.kagkarlsson.scheduler.task.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SelectForUpdatePollStrategy implements PollStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SelectForUpdatePollStrategy.class);
    private final Scheduler scheduler;
    private final PollingStrategyConfig pollingStrategyConfig;
    private AtomicBoolean moreExecutionsInDatabase = new AtomicBoolean(false);

    public SelectForUpdatePollStrategy(Scheduler scheduler, PollingStrategyConfig pollingStrategyConfig) {
        this.scheduler = scheduler;
        this.pollingStrategyConfig = pollingStrategyConfig;
    }

    @Override
    public void run() {
        Instant now = scheduler.clock.now();

        int lowerLimit = pollingStrategyConfig.getLowerLimit(scheduler.threadpoolSize);
        int upperLimit = pollingStrategyConfig.getUpperLimit(scheduler.threadpoolSize);
        int executionsToFetch = upperLimit - scheduler.currentlyProcessing.size(); // TODO: also check queue-length here

        // Might happen if upperLimit == threads and all threads are busy
        if (executionsToFetch == 0) {
            LOG.trace("No executions to fetch.");
            return;
        }

        // FIXLATER: should it fetch here if not under lowerLimit? probably
        List<Execution> pickedExecutions = scheduler.taskRepository.pickDue(now, executionsToFetch);
        LOG.trace("Picked {} taskinstances due for execution", pickedExecutions.size());

        moreExecutionsInDatabase.set(pickedExecutions.size() == executionsToFetch);

        if (pickedExecutions.size() == 0) {
            // No picked executions to execute
            LOG.trace("No executions due.");
            return;
        }

        for (Execution picked : pickedExecutions) {
            // TODO: refactor scheudler.executorService to a ExecutePickedService: add, get processing, get queue
            scheduler.executorService.execute(() -> {
                new ExecutePicked(scheduler, picked).run();
                if (moreExecutionsInDatabase.get()
                    && scheduler.currentlyProcessing.size() <= lowerLimit) { // TODO: we should compare to currentlyprocessing + queue-length here to allow for more prefetching
                    // TODO: howto prevent double trigger if executorservice have not yet had time to run the executions?
                    //       - maybe also checking queue length will fix it, because they will have been added to the queue..
                    scheduler.triggerCheckForDueExecutions();
                }
            });
        }
        scheduler.statsRegistry.register(StatsRegistry.SchedulerStatsEvent.RAN_EXECUTE_DUE);
    }
}
