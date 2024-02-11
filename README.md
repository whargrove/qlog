# qlog

qlog is a microservice that functions as an implementation of `tail` for files accessible to the service.

## Getting Started

qlog is a Java application using the Micronaut framework; Accordingly, we'll need to set up a few dependencies to get started.

This guide assumes you are using a modern version of macOS with `homebrew` installed.

1. Install sdkman
2. Initialize your shell to be sdkman aware
3. In the root directory of the project:
    ```shell
    sdk env install
    ```
   This will install the dependencies required to build and run the service. (Namely, the correct version of JDK.)

## Build

After the dependencies are installed build the project:

```shell
./gradlew build
```

This will initialize a new Gradle daemon and fetch the required dependencies from Maven Central repository and then build the project, including running the unit tests.

## Run with Gradle

Run the service directly with Gradle:

```shell
./gradlew run
```

Gradle will use the compiled JAR (from the build step) and run it. You can confirm the service is up when your shell prints:

```shell
Startup completed in 294ms. Server Running: http://localhost:8080
```

## Optional: Run with Docker

TODO: Add docker build instructions

## Prepare A Large Log File

TODO: Add python script to generate a large log file for practice.

## Tail A File

Query for lines from a file using `curl`:

```shell
curl -Ss "localhost:8080/queryLog?relativePath=access.log" | jq .
```

The service supports a trio of query params:

1. `relativePath` - Required, the file path relative to `/var/log` accessible to the service. This is the file that will be tailed by the service.
2. `lineCount` - Optional, default 42, the number of lines to return.
3. `filter` - Optional, default no op, the service will filter lines in the result set based on substring matching of the text in this filter against the line in the file.

Query parameter values must be URL encoded.

```shell
curl -Ss "localhost:8080/queryLog?relativePath=access.log&count=10&filter=DELETE%20%2Fscript.js" | jq .
```

## Benchmarking

If you're feeling spunky you can benchmark the service with `ab`: ApacheBench.

```shell
ab -c 8 -n 10000 "http://localhost:8080/queryLog?relativePath=access.log&count=100&filter=%20200%20"
This is ApacheBench, Version 2.3 <$Revision: 1903618 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking localhost (be patient)
Completed 1000 requests
Completed 2000 requests
Completed 3000 requests
Completed 4000 requests
Completed 5000 requests
Completed 6000 requests
Completed 7000 requests
Completed 8000 requests
Completed 9000 requests
Completed 10000 requests
Finished 10000 requests


Server Software:
Server Hostname:        localhost
Server Port:            8080

Document Path:          /queryLog?relativePath=access.log&count=100&filter=%20200%20
Document Length:        20674 bytes

Concurrency Level:      8
Time taken for tests:   3.667 seconds
Complete requests:      10000
Failed requests:        0
Total transferred:      207850000 bytes
HTML transferred:       206740000 bytes
Requests per second:    2727.22 [#/sec] (mean)
Time per request:       2.933 [ms] (mean)
Time per request:       0.367 [ms] (mean, across all concurrent requests)
Transfer rate:          55356.72 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.3      0       7
Processing:     2    3  15.3      2     539
Waiting:        1    2  15.3      2     539
Total:          2    3  15.3      2     539

Percentage of the requests served within a certain time (ms)
  50%      2
  66%      2
  75%      3
  80%      3
  90%      3
  95%      4
  98%      5
  99%      6
 100%    539 (longest request)
```

Tune the concurrency (-c), the total number of requests (-n), and the target URL to get a clear understanding of the performance characteristics of the service.
