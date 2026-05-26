mvn compile assembly:single
tornado --classpath target/assignment-gpu-1.0-jar-with-dependencies.jar nl.tue.pdc.Main --params=--raytracer