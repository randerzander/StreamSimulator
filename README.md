A project for streaming a log file into Kafka with several speed options.

Usage: java -jar target/StreamSimulator-1.0-SNAPSHOT.jar kafka_topic input brokerhost speed [delimiter] [datecol] ["Java DateFormat"]

Input must be a directory. Files will be ingested sequentially.

If speed is -1, the application will parse timestamps from the specified column and wait to produce each message based on the difference between timestamps in each line.

If speed is >=0, the application will sleep for that number of milliseconds between production of each message.
