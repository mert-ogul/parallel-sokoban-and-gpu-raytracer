mvn -q dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"
mvn -q package

$cp = (Get-Content target\classpath.txt) + ';target\classes'
tornado --classpath $cp --params="--change" nl.tue.pdc.Main