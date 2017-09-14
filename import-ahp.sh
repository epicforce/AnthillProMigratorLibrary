#!/bin/bash
#
# Script to import AHP's remoting libraries into a local repo.
# This is required for Maven to build and 'link' the AHP libraries
# to our plugin.
#
# Takes one argument: path to remoting 'lib' directory, which would
# usually be like /path/to/anthill3-dev-kit/remoting/lib
#
# Copryright 2017 Epic Force
# By sconley (sconley@epicforce.net)

if [ "$#" -ne 1 ] || ! [ -d "$1" ]; then
    echo "Usage: $0 /path/to/anthill3-dev-kit/remoting/lib" >& 2
    exit 1
fi

echo "We will use ahp-repo in the current directory as our maven repo."

# Clear out existing repo
echo "Deleting existing..."
rm -rf ahp-repo

# Remake it
echo "Making directory..."
mkdir ahp-repo

# get path
TARGET_PATH=`pwd`/ahp-repo

pushd "$1"

for x in *.jar; do
    echo "Processing file ${x}"
    mvn deploy:deploy-file \
        "-Durl=file://$TARGET_PATH" \
        -Dfile="$x" \
        -DgroupId=com.urbancode \
        -DartifactId="${x%.*}" \
        -Dpackaging=jar \
        -Dversion=1.0
done

popd

echo "Finished"
