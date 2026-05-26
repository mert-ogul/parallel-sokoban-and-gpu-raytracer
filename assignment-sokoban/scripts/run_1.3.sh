mvn clean compile assembly:single
java -jar target/assignment-sokoban-1.0-jar-with-dependencies.jar -p -t 8 "levels/screen.372"
java -jar target/assignment-sokoban-1.0-jar-with-dependencies.jar -p -t 8 --async-pool "levels/screen.372"

# these 2 commands for these levels: 372, 380, 401, 455 and these threads: 1, 2, 4, 8

