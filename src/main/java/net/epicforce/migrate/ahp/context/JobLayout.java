package net.epicforce.migrate.ahp.context;

/*
 * JobLayout.java
 *
 * So Anthill uses a weird tree graph structure to define its
 * job workflows, allowing for any number of parallelizations.
 *
 * This is good and all but makes it kind of difficult to
 * meaningfully re-structure how the jobs run and in what
 * ways.
 *
 * The JobLayout class provides a conversion from the arcane
 * way Anthill keeps track of it to something a little more
 * sane.
 *
 * There's a concept of a "Parallel Group" which is the
 * building block of this class.  A parallel group is a
 * group of jobs that can run in parallel, and is basically
 * a list of lists where each list is a 'stack' of jobs that
 * can run in parallel with the other stacks in the group.
 *
 * There will be at a minimum 1 parallel group with 1
 * stack in it (the simple case of a non-parallel job),
 * but there could be multiple groups, each with one or more
 * stacks.
 *
 * Bear in mind, each job may also have iterations and other
 * features that influence the way things are run.  This class
 * merely handles the layout.
 *
 * TODO: This doesn't really work right :P  I should maybe discard
 *       it instead of trying to process the weird graph that AHP
 *       uses.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.urbancode.anthill3.domain.workflow.WorkflowDefinition;
import com.urbancode.anthill3.domain.workflow.WorkflowDefinitionJobConfig;
import com.urbancode.commons.graph.TableDisplayableGraph;
import com.urbancode.commons.graph.Vertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JobLayout
{
    private final static Logger LOG = LoggerFactory.getLogger(JobLayout.class);

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    private List<ParallelGroup>                 groups = 
                                                new LinkedList<ParallelGroup>();
    private List<WorkflowDefinitionJobConfig>   iterableList;

    /**
     * Load a JobLayout from a WorkflowDefinition.
     *
     * @param def   The definition
     */
    public JobLayout(WorkflowDefinition def)
    {
        LOG.debug("Building JobLayout: {}", def);

        // Make a linked list we can iterate off of later.
        iterableList = new LinkedList<WorkflowDefinitionJobConfig>();

        // Might not be one
        if(def == null) {
            return;
        }

        // Extract the table graph
        TableDisplayableGraph<WorkflowDefinitionJobConfig> g =
                                def.getWorkflowJobConfigGraph();

        // What is the current Parallel Group
        ParallelGroup currentGroup = null;

        LOG.debug("Max depth of graph: {}", g.getMaxDepth());

        // Iterate over the table graph
        for(int depth = 1; depth <= g.getMaxDepth(); depth++) {
            // Get our list of vertices
            List<Vertex<WorkflowDefinitionJobConfig>> verts =
                g.getVerticesAtDepth(depth);

            int numVerts = verts.size();

            LOG.debug("At depth {} found {} verts", depth, numVerts);

            // if we're joining, we need a new vertex group.
            if((currentGroup == null) ||
               ((numVerts > 0) && (verts.get(0).isJoining()))) {
                if(currentGroup != null) {
                    groups.add(currentGroup);
                }

                currentGroup = new ParallelGroup(numVerts);

                // Skip this block if we're not debugging
                if(LOG.isDebugEnabled()) {
                    if(currentGroup == null) {
                        LOG.debug("Creating new ParallelGroup because we have "
                                  + "none yet."
                        );
                    } else {
                        LOG.debug("Creating new ParallelGroup due to join");
                    }
                }
            }

            // Iterate over verts and populate parallel group.
            for(int i = 0; i < numVerts; i++) {
                currentGroup.push(verts.get(i));
                iterableList.add(verts.get(i).getData());

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Adding vert {} to parallel group",
                              verts.get(i).getData().getName()
                    );
                }
            }
        }

        if(currentGroup != null) {
            groups.add(currentGroup);
        }

        LOG.debug("Finished building JobLayout");
    }

    /**
     * Returns the layout generated
     *
     * @return List of ParallelGroups
     */
    public List<ParallelGroup> getLayout()
    {
        return groups;
    }

    /**
     * Get all the layouts in an iterable list.
     *
     * @return LinkedList of WorkflowDefinitionJobConfig
     */
    public List<WorkflowDefinitionJobConfig> getAllJobs()
    {
        return iterableList;
    }

    /**
     * A Parallel Group, which is a structural class as explained in
     * the file header.
     *
     * @author sconley (sconley@epicforce.net)
     */
    public static class ParallelGroup
    {
        private ArrayList<WorkflowDefinitionJobConfig>[] stacks;

        /**
         * Initialize our parallel group.
         *
         * We use arrays because we know the width of the parallel
         * group at the time its started.
         *
         * @param numVerts - the number of vertices (width) of this
         *                   parallel group.
         */
        public ParallelGroup(int numVerts)
        {
            stacks = new ArrayList[numVerts];

            for(int i = 0; i < numVerts; i++) {
                stacks[i] = new ArrayList<WorkflowDefinitionJobConfig>();
            }
        }

        /**
         * Push a vertex's content into the correct stack for the
         * parallel group.  vertexes will be pushed into the most
         * empty stack, that's just how this graph thing works.
         *
         * @param v - The vertex to add to my stacks.
         */
        public void push(Vertex<WorkflowDefinitionJobConfig> v)
        {
            LOG.debug("Pushing vertex {} into parallel group", v);

            // We need so iterate backwards over our list to
            // find the most empty stack.
            for(int i = stacks.length-1; i >= 0; i--) {
                LOG.debug("Push loop iteration: {}", i);

                // Add our thing
                if((i == 0) || (stacks[i-1].size() > stacks[i].size())) {
                    LOG.debug("Found stack to push onto: {}", i);

                    stacks[i].add(v.getData());

                    for(int j = 1; j < v.getHeight(); j++) {
                        LOG.debug("...Adding padding");
                        stacks[i].add(null);
                    }

                    break;
                }
            }
        }

        /**
         * Get our stack
         *
         * @return the stacks
         */
        public List<WorkflowDefinitionJobConfig>[] getStacks()
        {
            return stacks;
        }
    }
}
