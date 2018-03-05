/**
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package com.yahoo.ycsb;

import org.apache.commons.lang.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * A thread that waits for the maximum specified time and then interrupts all the client
 * threads passed at initialization of this thread.
 * <p>
 * The maximum execution time passed is assumed to be in seconds.
 *
 * @author sudipto
 */
public class TerminatorThread extends Thread
{

    private final Collection<? extends Thread> threads;
    private long maxExecutionTime;
    private Workload workload;
    private long waitTimeOutInMS;

    private static final Logger log = LogManager.getLogger(TerminatorThread.class);

    public TerminatorThread(long maxExecutionTime, Collection<? extends Thread> threads,
                            Workload workload)
    {
        this.maxExecutionTime = maxExecutionTime;
        this.threads = threads;
        this.workload = workload;
        waitTimeOutInMS = 2000;
        log.error("Maximum execution time specified as: " + maxExecutionTime + " secs");
    }

    public void run()
    {
        StopWatch watch = new StopWatch();
        watch.start();
        long time = watch.getTime();
        long maxtime = maxExecutionTime * 1000;
        while (time < maxtime) {
            try {
                Thread.sleep(maxtime - time);
            } catch (InterruptedException e) {
                log.error("Could not wait until max specified time, TerminatorThread interrupted.");
                //return;
            }
            time = watch.getTime();
        }
        watch.stop();
        log.info("Maximum time elapsed. Requesting stop for the workload.");
        workload.requestStop();
        log.info("Stop requested for workload. Now Joining!");
        for (Thread t : threads) {
            while (t.isAlive()) {
                try {
                    t.join(waitTimeOutInMS);
                    if (t.isAlive()) {
                        log.info("Still waiting for thread " + t.getName() + " to complete. " +
                                                   "Workload status: " + workload.isStopRequested());
                        t.interrupt();
                    }
                } catch (InterruptedException e) {
                    log.trace("Thread Interrupted!");
                    // Do nothing. Don't know why I was interrupted.
                }
            }
        }
    }
}
