mvn -q dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt"
mvn -q package

tornado -cp target/classes nl.tue.pdc.sampling.RayBounceBenchmark