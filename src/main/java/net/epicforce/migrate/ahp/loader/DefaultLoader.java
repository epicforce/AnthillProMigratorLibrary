package net.epicforce.migrate.ahp.loader;

/*
 * DefaultLoader.java
 *
 * This is the default implementation of a loader, which will load the default
 * step classes that come with the library and extract properties from AHP.
 *
 * It would be reasonable to extend this instead of extending the AbstractLoader
 * directly if you wanted to have some kind of default fallback rather than
 * crashing if you choose not to support a given step.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.migrate.AbstractWorkflow;
import net.epicforce.migrate.ahp.migrate.AbstractJob;
import net.epicforce.migrate.ahp.migrate.AbstractStep;
import net.epicforce.migrate.ahp.exception.UnsupportedClassException;

public class DefaultLoader extends AbstractLoader
{
    /*****************************************************************
     * These methods return null because the default process does not
     * require them.
     ****************************************************************/

    public AbstractWorkflow loadWorkflowClass()
           throws UnsupportedClassException
    {
        return null;
    }

    public AbstractJob loadJobClass()
           throws UnsupportedClassException
    {
        return null;
    }

    /*****************************************************************
     * By default, we alter the step name and use it to load a class.
     ****************************************************************/
    public AbstractStep loadStepClass(final String stepName)
           throws UnsupportedClassException
    {
        // Make sure we can handle it.
        if(!stepName.startsWith("com.urbancode.anthill3.")) {
            throw new UnsupportedClassException(
                "Cannot process Anthill Step with class: " +
                stepName
            );
        }

        // Handoff
        return (AbstractStep)loadClass(
            stepName.replace(
                "com.urbancode.anthill3.",
                "net.epicforce.migrate.ahp.migrate.default."
            ) + "Migrate"
        );
    }
}
