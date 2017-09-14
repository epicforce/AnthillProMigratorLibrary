package net.epicforce.migrate.ahp;

/*
 * Migration.java
 *
 * This is the "main class" of the project which should be instanced by a
 * thread that wants to run an AHP migration.
 *
 * It handles the connection to Anthill Pro and keeps track of the context
 * of the migration.  Its configurable to use your own classes to alter
 * the behavior of the migration.
 *
 * Any exceptions thrown from this will be subclasses of MigrateException
 */

import java.lang.NullPointerException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.urbancode.anthill3.domain.persistent.PersistenceException;
import com.urbancode.anthill3.domain.project.Project;
import com.urbancode.anthill3.domain.project.ProjectFactory;
import com.urbancode.anthill3.domain.security.AuthorizationException;
import com.urbancode.anthill3.domain.step.StepConfig;
import com.urbancode.anthill3.domain.step.StepConfigException;
import com.urbancode.anthill3.domain.workflow.Workflow;
import com.urbancode.anthill3.domain.workflow.WorkflowDefinitionJobConfig;
import com.urbancode.anthill3.domain.workflow.WorkflowFactory;
import com.urbancode.anthill3.main.client.AnthillClient;
import com.urbancode.anthill3.persistence.UnitOfWork;

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.context.JobLayout;
import net.epicforce.migrate.ahp.exception.*;
import net.epicforce.migrate.ahp.loader.AbstractLoader;
import net.epicforce.migrate.ahp.migrate.AbstractWorkflow;
import net.epicforce.migrate.ahp.migrate.AbstractJob;
import net.epicforce.migrate.ahp.migrate.AbstractStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See constructor for calling notes.
 *
 * This class must be 'close()'d when its done.
 *
 * NOTE : This is not threadsafe for the unlikely case of multiple
 *        different keystores being used for different AHP environments.
 *        It is up to the caller to make this threadsafe is they want
 *        to do that.  After construction, all further calls *are*
 *        threadsafe.  As long as you're reusing the same keystore
 *        (or not using a keyestore) you have nothing to worry about.
 *
 * @author sconley (sconley@epicforce.net)
 */
public class Migration implements Runnable
{
    private final static Logger LOG = LoggerFactory.getLogger(Migration.class);

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    /*
     * Our anthill client for this thread.
     */
    protected AnthillClient     client;

    /*
     * Our context for the migration, which will ultimately be
     * the result of the overall migration.
     */
    protected AbstractContext   context = null;

    /*
     * Status - for threading purposes
     */
    protected int               status = 0;

    /*
     * Keep track of our error (if we've got one)
     */
    protected MigrateException  error = null;

    /*
     * What workflow ID are we migrating?
     */
    protected Long              workflowId = null;

    /*
     * What's its name?  Will be null if we haven't loaded it yet.
     */
    protected String            workflowName = null;

    /*
     * Our loader
     */
    protected AbstractLoader    loader = null;

    /*
     * To compute progress
     */
    protected int               numSteps = 0;
    protected int               migratedStepCount = 0;

    /*****************************************************************
     * CONSTANTS
     ****************************************************************/

    /*
     * Status constants
     */
    public static final int     NEED_SETUP = 0;
    public static final int     READY = 1;
    public static final int     RUNNING = 2;
    public static final int     ERROR = 3;
    public static final int     SUCCESS = 4;
    public static final int     CLOSED = -1;

    /*****************************************************************
     * ACCESSORS
     ****************************************************************/

    /**
     * @return the context (result) of migration
     */
    public AbstractContext getContext()
    {
        return context;
    }

    /**
     * @param context    A context to set on this migration object.
     *
     * This is used to initialize the migration.
     */
    public void setContext(AbstractContext context)
    {
        this.context = context;
    }

    /**
     * @param loader   A loader to use for this migration.
     *
     * This is used to initialize the migration.
     */
    public void setLoader(AbstractLoader loader)
    {
        this.loader = loader;
    }

    /**
     * @return a status integer (see status constants above)
     */
    public final int getStatus()
    {
        return status;
    }

    /**
     * Get an error object, or null if no error at the moment
     *
     * @return an error object if set, or null
     */
    public final MigrateException getError()
    {
        return error;
    }

    /**
     * The purpose for setting an error is to allow a thread supervisor to
     * set an error on this clase in case of a failure that causes the
     * thread to crash.  It's a book-keeping sort of thing to ease logic
     * flow.
     *
     * NOTE: I wound up doing this a different way, but I've kept it in
     * case its useful.
     *
     * @param error   error to set.
     */
    public void setError(MigrateException error)
    {
        this.error = error;
        this.status = ERROR;
    }

    /**
     * @param workflowId    The workflow to migrate.
     *
     * This is used as part of the init process.
     */
    public void setWorkflowId(final Long workflowId)
    {
        if((status == NEED_SETUP) && (workflowId != null)) {
            status = READY;
        }

        this.workflowId = workflowId;
    }

    /**
     * @return the workflow ID we're migrating.
     */
    public final Long getWorkflowId()
    {
        return this.workflowId;
    }

    /**
     * @return workflow name or null if we haven't loaded it yet.
     */
    public final String getWorkflowName()
    {
        return workflowName;
    }

    /**
     * Get computed "percent done" as an integer from 1 to 100.
     *
     * @return a number from 1 to 100
     */
    public int getProgress()
    {
        if((status == SUCCESS) || (status == ERROR) || (status == CLOSED)) {
            return 100;
        }

        if(numSteps == 0) {
            return 0;
        }

        int ret = (int)Math.round(
                        ((double)migratedStepCount/(double)numSteps)*100
        );

        // don't allow this to be 100
        if(ret >= 100) {
            return 99;
        } else {
            return ret;
        }
    }

    /*****************************************************************
     * CONSTRUCTORS
     ****************************************************************/

    /**
     * Constructor: takes AHP connection information.  It will try to
     * connect right away.
     *
     * @param host          AHP Host name
     * @param port          AHP Remoting Port
     * @param username      AHP User name
     * @param password      AHP Password
     * @param keystorePath  Keystore path, if used.
     * @param keystorePass  Keystore password, if used.
     *
     * @throws MigrateException on error -- probably a ConnectException
     *
     */
    public Migration(final String host, int port, final String username,
                     final String password, final String keystorePath,
                     final String keystorePass)
            throws MigrateException {
        try {
            LOG.debug("Contructing Migration object");

            // Set properties for keystore load
            if((keystorePath != null) && (keystorePath.length() > 0)) {
                System.setProperty("anthill3.client.ssl.keystore.pwd",
                                   keystorePass);
                System.setProperty("anthill3.client.ssl.keystore",
                                   keystorePath);
            } else {
                System.clearProperty("anthill3.client.ssl.keystore.pwd");
                System.clearProperty("anthill3.client.ssl.keystore");
            }

            // Try to connect
            client = AnthillClient.connect(host, port, username, password);

            if(client == null) {
                LOG.error("AnthillClient returned null -- probably bad " +
                          "credentials.");
                throw new ConnectException(
                    "Could not Connect to Anthill Pro.  This is probably "
                    + "a username/password problem."
                );
            }
        } catch(AuthorizationException e) {
            LOG.error("Caught exception", e);
            throw new ConnectException("Could not connect to Anthill Pro", e);
        } catch(NullPointerException e) {
            LOG.error("A NullPointerException was thrown -- this is " +
                      "probably an AHP connection problem (hostname?)");
            // AHP throws null pointer exceptions if the host name is
            // wrong (among other potential problems that I guess AHP
            // doesn't error check for)
            throw new ConnectException(
                "One of your settings (probably hostname) is incorrect.  "
                + "Please double-check and try again."
            );
        }
    }

    /**
     * Constructor: with default keystorePass ("changeit")
     *
     * @param host          AHP Host name
     * @param port          AHP Remoting Port
     * @param username      AHP User name
     * @param password      AHP Password
     * @param keystorePath  Keystore path, if used.
     *
     * @throws MigrateException on error -- probably a ConnectException
     */
    public Migration(final String host, int port, final String username,
                     final String password, final String keystorePath)
            throws MigrateException {
        this(host, port, username, password, keystorePath, "changeit");
    }

    /**
     * Constructor: no keystore
     *
     * @param host          AHP Host name
     * @param port          AHP Remoting Port
     * @param username      AHP User name
     * @param password      AHP Password
     *
     * @throws MigrateException on error -- probably a ConnectException
     */
    public Migration(final String host, int port, final String username,
                     final String password)
            throws MigrateException {
        this(host, port, username, password, null, null);
    }

    /*****************************************************************
     * DESTRUCTOR
     *
     * Closes things up and makes sure the Anthill client isn't
     * clinging to the thread anymore.
     ****************************************************************/

    /**
     * Close up and tidy -- this will not throw an exception.
     *
     * This class will be unusable after running this; you will get
     * NPE's if you try to call a closed Migration.
     *
     * This does not dispose of the context as in some cases it
     * may remain useful after termination.
     */
    public void close()
    {
        LOG.debug("Closing Migration object.  Thank you! <3");

        try {
            client.unbind();
        } catch(Exception e) { }

        try {
            client.disconnect();
        } catch(Exception e) { }

        client = null;
        status = CLOSED;
    }

    /*****************************************************************
     * QUERIES
     *
     * These are "friendly" methods to query Anthill and get results
     * that do not rely on Anthill's arcane objects but can be used
     * to build UI's.
     ****************************************************************/

    /**
     * The purporse of this method is to take a project name query
     * and return a map of project names to Workflow name/ID pairs.
     *
     * This can be used to drive a UI To find workflows to migrate.
     *
     * @param project  The query string
     * @param limit    Limit the results to the provided number of
     *                 projects.  Can be 0 for no limit.
     *
     * The purpose of the "limit" is because this query process
     * can be incredibly slow.  Anthill does not provide an
     * efficient access method, and it WILL take an order of
     * *minutes* to run this query.  Unless you are running the
     * migration in the same datacenter as the AHP server, you
     * should limit this to around 30 or you may never return.
     *
     * All results will be returned alphabetically sorted using
     * a linked map.
     *
     * @return a map of project names to workflow name/ID pairs.
     * @throws MigrateException on any kind of error.
     */
    public Map<String, Map<String, Long>>
           fetchWorkflowsForProjectName(final String project,
                                        int limit)
           throws MigrateException
    {
        // For our transaction
        UnitOfWork uow = null;

        LOG.debug("fetchWorkflowsForProjectName: {}, {}", project, limit);

        try {
            // Start a transaction
            uow = client.createUnitOfWork();

            // Get project list
            Project[] projects = ProjectFactory.getInstance()
                                               .restoreAllLikeName(project);

            LOG.debug("Got {} results", projects.length);

            // apply limit if necessary
            if((limit > 0) && (projects.length > limit)) {
                LOG.error("Got too many results - {} out of {}",
                          projects.length, limit);
                throw new MigrateException(
                    "Returned too many results: " +
                    String.valueOf(projects.length) +
                    " with limit of " +
                    String.valueOf(limit)
                );
            }

            // Allocate memory
            LinkedHashMap<String, Map<String, Long>> ret =
              new LinkedHashMap<String, Map<String, Long>>(projects.length);

            // Process results
            for(Project p : projects) {
                // Grab workflows (originating)
                Workflow[] workflows = p.getOriginatingWorkflowArray();

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Project {} got {} results", p.getName(),
                                                           workflows.length
                    );
                }

                // Add to our map
                LinkedHashMap<String, Long> wfm =
                    new LinkedHashMap<String, Long>(workflows.length);

                for(Workflow w : workflows) {
                    wfm.put(w.getName(), w.getId());

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("...Workflow: {}", w.getName());
                    }
                }

                ret.put(p.getName(), wfm);
            }

            // Done!
            return ret;
        } catch(PersistenceException e) {
            LOG.error("Caught exception", e);
            throw new MigrateException("Got a persistence error from AHP: ",
                                       e);
        } catch(AuthorizationException e) {
            LOG.error("Caught exception", e);
            throw new ConnectException("Failed authorization with AHP: ",
                                       e);
        } finally {
            // Clean up the UOW
            if(uow != null) {
                try {
                    uow.cancel();
                } catch(Exception e) { }

                uow.close();
                uow = null;
            }
        }
    }

    /*****************************************************************
     * MIGRATE
     *
     * Actually run the migration.  This is designed to run in its own
     * thread if you want; this is optional, however.
     ****************************************************************/

    /**
     * This can be run as a thread which will exit when the migration
     * is complete, or it can run inline with the calling thread.
     *
     * Because we're making a Runnable, this doesn't return a result
     * and instead a separate call must be made.
     *
     * You need to set a workflow ID with setWorkflowId before
     * running this, and a loader and a context with setContext
     * and setLoader.
     *
     * This will attempt to be as well behaved as possible, and never
     * throw an exception; rather, the "getError()" will be populated on
     * error and the "getStatus()" set to the ERROR code.
     */
    public void run() {
        // Abort if not ready.
        if(status != READY) {
            // These are both programmer errors and should never be seen
            // by end users if the library is properly programmed.
            if(status == NEED_SETUP) {
                LOG.error("Tried to Migrate.run() on unsetup Migrate class");
                error = new MigrateException(
                            "Migrate is not in a ready state -- did you " +
                            "remember to use setWorkflowId before running?"
                );
            } else {
                LOG.error("Tried to Migrate.run() on a used Migrate class");
                error = new MigrateException(
                            "This Migrate object is already in use and " +
                            "cannot be re-run.  Please instance a new " +
                            "Migrate class."
                );
            }

            status = ERROR;
            return;
        }

        LOG.debug("run() called -- and we're off!  Workflow ID: {}",
                  workflowId
        );

        // For our transaction
        UnitOfWork uow = null;

        // And let's go!
        status = RUNNING;

        try {
            // bind our thread
            client.bind();

            // Create our unit of work
            uow = client.createUnitOfWork();

            // try to load our workflow
            Workflow wf = WorkflowFactory.getInstance()
                                         .restore(workflowId);

            if(wf == null) {
                LOG.error("Workflow {} does not exist!", workflowId);
                throw new MigrateException("Workflow ID " +
                                           String.valueOf(workflowId) +
                                           " does not exist!");
            }

            // Set our name
            workflowName = wf.getName();

            /*
             * Jobs in AHP use a "configuration graph".  The configuration
             * graph allows any combination of parallel / sequential builds.
             *
             * This is pretty complex to represent and iterate over, but
             * we need to visit each job in sequence and put them together
             * in a useful structure.
             */
            LOG.debug("Loading job layout");
            JobLayout layout = new JobLayout(wf.getWorkflowDefinition());

            // Push the workflow into our context
            context.setWorkflow(wf);
            context.setClient(client);
            context.setLayout(layout);

            // Grab our Workflow and Job runner if we have one
            AbstractWorkflow wfRunner = loader.loadWorkflowClass();

            if(wfRunner != null) {
                LOG.debug("Running Workflow pre-run step");
                wfRunner.preRun(context);
            }

            numSteps = layout.getAllJobs().size();

            LOG.debug("Job layout got {} steps", numSteps);

            // Iterate over all of them
            for(WorkflowDefinitionJobConfig job : layout.getAllJobs()) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Processing job: {}", job.getName());
                }

                context.setCurrentJob(job);

                // Run job callback if we've got them
                AbstractJob jobRunner = loader.loadJobClass();

                try {
                    if(jobRunner != null) {
                        LOG.debug("Running Job pre-run step");
                        jobRunner.preRun(context);
                    }

                    // Iterate over steps in job.
                    for(StepConfig step : job.getJobConfig()
                                             .getActiveStepConfigArray()) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Processing step: {}, class {}",
                                      step.getName(),
                                      step.getClass().getName()
                            );
                        }

                        context.setCurrentStep(step);
                        AbstractStep stepLoader = loader.loadStepClass(
                                            step.getClass().getName()
                        );
                        try {
                            stepLoader.run(context);
                        } catch(SkipException e) {
                            // swallow the skip exception
                            LOG.warn("Skipping step: {}", e.getMessage());
                        }
                    }

                    // And the post callback if we've got it.
                    if(jobRunner != null) {
                        LOG.debug("Running Job post-run step");
                        jobRunner.postRun(context);
                    }
                } catch(SkipException e) {
                    // Swallow job skip exception
                    LOG.warn("Skipping step: {}", e.getMessage());
                }

                migratedStepCount++;
            }

            // Run post run
            if(wfRunner != null) {
                LOG.debug("Running Workflow post-run step");
                wfRunner.postRun(context);
            }

            this.status = SUCCESS;
            LOG.debug("Successfully completed!");
        } catch(AuthorizationException e) {
            LOG.error("Caught exception", e);
            this.error = new ConnectException("Authorization error from AHP: ",
                                              e);
            this.status = ERROR;
        } catch(PersistenceException | StepConfigException e) {
            LOG.error("Caught exception", e);
            this.error = new MigrateException("Persistence error from AHP: ",
                                              e);
            this.status = ERROR;
        } catch(MigrateException e) {
            LOG.error("Caught exception", e);
            this.error = e;
            this.status = ERROR;
        } catch(Exception e) {
            LOG.error("General exception", e);

            if(e.getMessage() != null) {
                this.error = new MigrateException("General error: " + 
                                                  e.getMessage(), e
                );
            } else {
                this.error = new MigrateException("General Error: " +
                                                  e.getClass().getName(), e
                );
            }

            this.status = ERROR;
        } finally {
            client.unbind();

            // close out unit of work
            if(uow != null) {
                try {
                    uow.cancel();
                } catch(Exception e) { }

                uow.close();
                uow = null;
            }
        }
    }
}
