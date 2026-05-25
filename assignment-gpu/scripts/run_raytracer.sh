mvn -q dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"
mvn -q -DskipTests package "-Dmaven.compiler.source=25" "-Dmaven.compiler.target=25"

$cp = (Get-Content target\classpath.txt) + ';target\classes'
tornado --classpath $cp --params="--raytracer" nl.tue.pdc.Main