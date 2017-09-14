package net.epicforce.migrate.ahp.migrate;

/*
 * AbstractWorkflow.java
 *
 * This is an optional class that can be used to run stuff at the
 * start / finish of a workflow migration.  This is basically a way
 * to inject pre / post steps into the migration process.
 *
 * The Context will be provided.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.exception.MigrateException;

public abstract class AbstractWorkflow
{
    /**
     * This must be implemented by each step.
     *
     * The step will be assumed to run successfully unless an exception
     * is thrown.
     *
     * This is run before any other migration step -- before jobs are
     * processed, etc.
     *
     * If you need to do some kind of set up before a workflow is
     * migrated, you can do it here.  'workflow' will be populated
     * in context but nothing else.
     *
     * @param context   The migration context
     * @throws MigrateException on any error
     */
    public abstract void preRun(AbstractContext context)
           throws MigrateException;


    /**
     * This must be implemented by each step.
     *
     * The step will be assumed to run successfully unless an exception
     * is thrown.
     *
     * If you need to do some kind of clean up at the end of the
     * migration, you can do it here.  You will receive the context
     * in whatever state its in just before the migration finishes.
     *
     * @param context   The migration context
     * @throws MigrateException on any error
     */
    public abstract void postRun(AbstractContext context)
           throws MigrateException;
}
