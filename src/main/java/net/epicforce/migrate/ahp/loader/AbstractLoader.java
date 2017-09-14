package net.epicforce.migrate.ahp.loader;

/*
 * AbstractLoader.java
 *
 * The way the migration tool works, is it loads a class for each
 * AHP step along with special classes for dealing with pre/post
 * processing of the context.
 *
 * The loader does the loading of these step classes.  It's likely
 * each migration type will need to define its own set of step
 * classes in order to massage the data from AHP in a way that
 * conforms to what the target desires.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;

import net.epicforce.migrate.ahp.migrate.AbstractWorkflow;
import net.epicforce.migrate.ahp.migrate.AbstractJob;
import net.epicforce.migrate.ahp.migrate.AbstractStep;
import net.epicforce.migrate.ahp.exception.UnsupportedClassException;

public abstract class AbstractLoader
{
    /**
     * The workflow loader will attempt to load a workflow class
     * and return it.
     *
     * This MAY return null if there is no need for a workflow
     * class, and the caller should be prepared for that.
     *
     * @return implementation of AbstractWorkflow or null
     *
     * @throws UnsupportedClassException if class won't load.
     */
    public abstract AbstractWorkflow loadWorkflowClass()
           throws UnsupportedClassException;

    /**
     * The job loader will attempt to load a job class
     * and return it.
     *
     * This MAY return null if there is no need for a job
     * class, and the caller should be prepared for that.
     *
     * The job name, etc. will be passed via the context.
     *
     * @return implementation of AbstractJob or null
     *
     * @throws UnsupportedClassException if class won't load.
     */
    public abstract AbstractJob loadJobClass()
           throws UnsupportedClassException;

    /**
     * The step loader will attempt to load a step class
     * based on an Anthill step class name that is provided.
     *
     * This MAY NOT return null and must return something
     * or raise UnsupportedClassException
     *
     * @param stepName   Full name of step class
     *
     * @return implementation of AbstractStep
     *
     * @throws UnsupportedClassException if class won't load.
     */
    public abstract AbstractStep loadStepClass(final String stepName)
           throws UnsupportedClassException;

    /**
     * This helper method loads a class based on a class name string
     * and is probably the underlying implementation you'll want to
     * use for your own implementations of the above methods.
     *
     * It assumes the class it is instancing takes no parameters.
     *
     * @param className     Name of class to load
     * @return Object instance of class
     * @throws UnsupportedClassException if class won't load.
     */
    protected Object loadClass(final String className)
              throws UnsupportedClassException
    {
        try {
            return Class.forName(className)
                        .getDeclaredConstructor()
                        .newInstance();
        } catch(NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            throw new UnsupportedClassException(
                "Unsupported class: " + className, e);
        }
    }
}
