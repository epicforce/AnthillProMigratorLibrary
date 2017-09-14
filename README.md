# Anthill Pro Migrator Library

The purpose of this library is to provide a framework for migrating Anthill Pro to other systems.  Epic Force built it as a basic framework for our other migration products, specifically Anthill Pro to Jenkins and Anthill Pro to IBM UrbanCode Build.

The library doesn't do much on its own at the moment; its mostly base classes that must be extended by your own migration tool.


BUILDING
========
In order to build this library, you must first fetch a copy of Anthill's remoting API.  This is usually downloadable under the "tools" link at the top right corner of your Anthill Pro install.

PLEASE NOTE: Different versions of AHP have different remoting API's.  They are NOT compatible with each-other.  Therefore, you must build this plugin for whatever version of AHP you anticipate interfacing with.  You may get errors about serial numbers not matching if you try to use mis-matched remoting API's.

In order for Maven to build using these external JARs, we build a local repository.  This is done quite simply with the import-ahp.sh job.  Run it thusly:

```
./import-ahp.sh /path/to/anthill-devkit/remoting/lib
```

You want the 'remoting/lib' directory contained in the devkit ZIP.  Run it once, and you should never have to run it again, unless you want to build against a different version of AHP remoting API.

Then you can do a typical:

```
mvn clean install
```

to build the library and put it in your local Maven repo.  It'll be ready for use by the Jenkins migration plugin or any of our other migration tools.


HOW TO USE
==========
If you want to make your own migration, you can use this library as a base.  Unfortunately, you will still need a pretty intimate knowledge of the internals of Anthill; fortunately, Anthill's remoting API is a pretty in-depth representation of how things work and you can read their documentation on the 'tools' page mentioned above.  Its also included in the remoting API zip file.

The main class here is the Migration class, and it acts as a governor to dictate how your migration runs.  You provide it callbacks which are called at different stages of the migration; before the workflow, for each job, for each step, and then at the end of each job, and the end of each workflow.  You define a loader class which you provide to the Migration tool in order to make this work.

TODO: Document this fully, but for now, you can see how the Jenkins plugin works.


FUTURE PLANS
============

* Bring more common features from the Jenkins and UCB Migration Tools into the common library.  I haven't taken the time to identify what to move yet, but I'm sure there's some.

* Make a "default migration" that will extract properties and maybe some sort of YAML or XML format dump of the workflows.  You will see references to a "default migration", that's what this refers to.  It is pretty incomplete though.
