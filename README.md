A project for streaming files from a directory into [Apache NiFi](http://nifi.apache.org/) or [Apache Kafka](http://kafka.apache.org/) with several speed options.

Input must be a directory. Files will be ingested sequentially.

If speed is -1, the application will parse timestamps from the specified column and wait to produce each message based on the difference between timestamps in each line.

If speed is >=0, the application will sleep for that number of milliseconds between production of each message.

Usage: java -jar target/StreamSimulator-1.0-SNAPSHOT.jar input_dir host topic speed [delimiter] [datecol] ["Java DateFormat"]
Push to NiFi over S2S Example
```
$ java -jar target/StreamSimulator-1.0-SNAPSHOT.jar ~/data/gas/1/ localhost:8080 gas 100
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Sending: gas:localhost:8080:0.00    0.00    0.00    -50.85  -1.95   -41.82  1.30    -4.07   -28.73  -13.49  -3.25   55139.95 50669.50 9626.26 9762.62 24544.02 21420.68 7650.61 6928.42
Bytes transferred: 155
Sending: gas:localhost:8080:0.01    0.00    0.00    -49.40  -5.53   -42.78  0.49    3.58    -34.55  -9.59   5.37    54395.77 50046.91 9433.20 9591.21 24137.13 20930.33 7498.79 6800.66
Bytes transferred: 155
...
Finished ingesting file: /Users/randy/data/gas/1/test1.txt
Sending: gas:localhost:8080:0.00    0.00    0.00    -50.85  -1.95   -41.82  1.30    -4.07   -28.73  -13.49  -3.25   55139.95 50669.50 9626.26 9762.62 24544.02 21420.68 7650.61 6928.42
Bytes transferred: 155
Sending: gas:localhost:8080:0.01    0.00    0.00    -49.40  -5.53   -42.78  0.49    3.58    -34.55  -9.59   5.37    54395.77 50046.91 9433.20 9591.21 24137.13 20930.33 7498.79 6800.66
Bytes transferred: 155
Finished ingesting file: /Users/randy/data/gas/1/test2.txt
```
