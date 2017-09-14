package net.epicforce.migrate.ahp.context;

import com.urbancode.anthill3.domain.step.StepConfig;
import com.urbancode.anthill3.domain.workflow.Workflow;
import com.urbancode.anthill3.domain.workflow.WorkflowDefinitionJobConfig;
import com.urbancode.anthill3.main.client.AnthillClient;


/*
 * AbstractContext.java
 *
 * So the 'context' of a migration is a running accumulation of the
 * state of that migration.  What, exactly, a context consists of is
 * ultimately up to the implementor of a particular process.
 *
 * The context will be passed to each phase of the migration process
 * and it can be returned by the Migration if desired after a run.
 *
 * @author sconley (sconley@epicforce.net)
 */
public abstract class AbstractContext
{
    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    /*
     * These objects from AHP basically are the state of the
     * migration.  While not usually used by steps, they are important
     * for back-tracking errors in particular.
     */
    protected Workflow                      workflow = null;
    protected WorkflowDefinitionJobConfig   currentJob = null;
    protected StepConfig                    currentStep = null;
    protected AnthillClient                 client = null;
    protected JobLayout                     layout = null;

    /*
     * Accessors for our workflow / current job / current step.
     */
    public final Workflow getWorkflow()
    {
        return workflow;
    }

    public void setWorkflow(final Workflow workflow)
    {
        this.workflow = workflow;
    }

    public final WorkflowDefinitionJobConfig getCurrentJob()
    {
        return currentJob;
    }

    public void setCurrentJob(final WorkflowDefinitionJobConfig job)
    {
        this.currentJob = job;
    }

    public final StepConfig getCurrentStep()
    {
        return currentStep;
    }

    public void setCurrentStep(final StepConfig step)
    {
        this.currentStep = step;
    }

    public AnthillClient getClient()
    {
        return client;
    }

    public void setClient(AnthillClient client)
    {
        this.client = client;
    }

    public JobLayout getLayout()
    {
        return layout;
    }

    public void setLayout(JobLayout layout)
    {
        this.layout = layout;
    }

    /*****************************************************************
     * ABSTRACT METHODS
     ****************************************************************/

}
