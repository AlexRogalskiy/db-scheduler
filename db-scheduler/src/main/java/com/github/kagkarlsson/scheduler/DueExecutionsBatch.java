/**
 * Copyright (C) Gustav Karlsson
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
package com.github.kagkarlsson.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

class DueExecutionsBatch {

    private static final Logger LOG = LoggerFactory.getLogger(DueExecutionsBatch.class);
    private final int generationNumber;
    private final AtomicInteger executionsLeftInBatch;
    private final Predicate<Integer> whenToTriggerCheckForNewBatch;
    private int threadpoolSize;
    private boolean possiblyMoreExecutionsInDb;
    private boolean stale = false;
    private boolean triggeredExecuteDue;

    public DueExecutionsBatch(int threadpoolSize, int generationNumber, int executionsAdded, boolean possiblyMoreExecutionsInDb, Predicate<Integer> whenToTriggerCheckForNewBatch) {
        this.threadpoolSize = threadpoolSize;
        this.generationNumber = generationNumber;
        this.possiblyMoreExecutionsInDb = possiblyMoreExecutionsInDb;
        this.executionsLeftInBatch = new AtomicInteger(executionsAdded);
        this.whenToTriggerCheckForNewBatch = whenToTriggerCheckForNewBatch;
    }

    public void markBatchAsStale() {
        this.stale = true;
    }

    /**
     *
     * @param triggerCheckForNewBatch may be triggered more than one in racy conditions
     */
    public void oneExecutionDone(Supplier<Boolean> triggerCheckForNewBatch) {
        executionsLeftInBatch.decrementAndGet();

        LOG.trace("Batch state: stale:{}, triggeredExecuteDue:{}, possiblyMoreExecutionsInDb:{}, executionsLeftInBatch:{}, whenToTriggerCheckForNewBatch:{}",
                stale, triggeredExecuteDue, possiblyMoreExecutionsInDb, executionsLeftInBatch.get(), whenToTriggerCheckForNewBatch.test(executionsLeftInBatch.get()));
        if (!stale
                && !triggeredExecuteDue
                && possiblyMoreExecutionsInDb
                && whenToTriggerCheckForNewBatch.test(executionsLeftInBatch.get())) {
            triggeredExecuteDue = triggerCheckForNewBatch.get();
        }
    }

    public boolean isOlderGenerationThan(int compareTo) {
        return generationNumber < compareTo;
    }

    public int getGenerationNumber() {
        return generationNumber;
    }
}
