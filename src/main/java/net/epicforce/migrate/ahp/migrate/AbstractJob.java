package net.epicforce.migrate.ahp.migrate;

/*
 * AbstractJob.java
 *
 * This is an optional class that can be used to run stuff at the start/finish
 * of a job migration.  The migration works by iterating over all the jobs
 * in AHP and within each job iterating over each step, running the
 * step classes.
 *
 * At the top/bottom of a particular job's iteration, this can be used to
 * inject calls.
 *
 * The Context will be provided.  Each job gets a FRESH copy of this
 * object, so any class properties will be 'clean' at the top of each
 * loop.  Use the context if you want something to persist from job to
 * job.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.exception.MigrateException;

public abstract class AbstractJob
{
    /**
     * This must be implemented by each step.
     *
     * The step will be assumed to run successfully unless an exception
     * is thrown.
     *
     * This is run at the top of the loop of steps for a given job.
     *
     * If you need to do some kind of set up before a job is
     * migrated, you can do it here.  'workflow' will be populated
     * in context along with 'currentJob'.
     *
     * @param context the migration context
     * @throws MigrateException on any failure.
     */
    public abstract void preRun(AbstractContext context)
           throws MigrateException;


    /**
     * This must be implemented by each step.
     *
     * The step will be assumed to run successfully unless an exception
     * is thrown.
     *
     * This is run at the 'bottom' of the loop after all steps have been
     * processed for a given job.  'currentJob' and 'workflow' will be
     * in context, and the context will be in whatever state it was put
     * in by the step migrations.
     *
     * @param context the migration context
     * @throws MigrateException on any failure.
     */
    public abstract void postRun(AbstractContext context)
           throws MigrateException;
}
