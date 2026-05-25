mvn -q dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"
mvn -q -DskipTests package "-Dmaven.compiler.source=25" "-Dmaven.compiler.target=25"

tornado -cp target/classes nl.tue.pdc.sampling.RayBounceBenchmark