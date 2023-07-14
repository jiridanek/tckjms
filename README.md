# JMS TCK tests (Gradle)

Helper Gradle scripts to run Jakarta JMS TCK with ActiveMQ.

## Prerequisites

`patch` command needs to be installed

    yum install patch

Suitable broker must be installed and configured.
The config depends on client used, see `buildSrc/main/kotlin/*.kt` files for what is expected.
Namely, clients are configured to log-in as tckuser/tckuser, and access the broker using default port for each protocol.

## Gradle Essentials

Read the minimal introduction to Gradle which will be written soon.

Executing Gradle:

    ./gradlew    # on Linux
    gradlew.bat  # on Windows

(If you don't have gradle installed, the script will fetch gradle from the internet automatically.)

## Example invocations

run some TCKs (start broker first)

    ./gradlew -PjmsVersion=2.0 -PjmsClient=qpid-jms -PjmsBroker=activemq-artemis -Pupstream addDestinations
    ./gradlew -PjmsVersion=2.0 -PjmsClient=qpid-jms -PjmsBroker=activemq-artemis -Pupstream runTck
    ./gradlew -PjmsVersion=2.0 -PjmsClient=qpid-jms -PjmsBroker=activemq-artemis -Pupstream deleteDestinations

cleaning the build directory

    ./gradlew clean

skip re-download while developing, use `-x downloadTck`, e.g.

    ./gradlew patchTckConfig -x downloadTck

list all top level tasks defined in build file

    ./gradlew tasks --all 

### Tasks

Tasks have dependencies. Invoking a task causes all dependent tasks to be invoked first, unless Gradle can tell that it is already satisfied (if their build inputs have not changed and their build outputs are already present and unchanged from after last execution).

Lets say `a <- b` means `b` depends on `a`. Then for example, `downloadTck <- unzipTck`.

There are three top level tasks, `fetchTck`, `patchTck` and `runTck`. Then there are many tasks these three depend on.

### Properties

Which properties are required depends on the task. For example, properties `host` and `port` are used only for the `runTck` task. See the example above how to invoke `runTck` task.

* -PjmsVersion
    * `1.1`
    * `2.0`

* -PjmsClient
    * `qpid-jms` // qpid-jms (AMQP)
    * `activemq-client` // activemq-client (OpenWire)
    * `artemis-jms-client` // artemis-jms-client (Core)

* -PjmsBroker
    * `activemq-artemis` // ActiveMQ Artemis
    * `activemq`  // ActiveMQ Classic

* -PjmsClientVersion
    * default is `+`, meaning latest upstream version, use something like `0.11.0.redhat-1`

* -Phost, optional
    * broker hostname (without port), default is `localhost`

* -Pport, optional
    * broker port number, default depends on client, either `5672` or `61616`

* <repository>
  * -Pupstream
  * -Plocal
    * maven local repository in `~/.m2/repository`
  * -Psystem
    * rpms installed to `file:///usr/share/java/maven-repo`
  * -Prelease, -Pcandidate
    * Red Hat public repositories for released versions

## Return code

The gradle build will fail with return code 1 if any of the ant invocations fail. Ant will fail if there are some failing TCK tests. Test result can be best determined by analyzing file `runclient.log`. This is done by a python script, which writes to file `summary.txt` in the `/tmp/JTreport/text/` directory.

The build will *not* fail if applying patch files partially fails.

# Run single TCK test

go to `core/` or `core20/` and run `ant`

    cd build/messaging-tck/src/com/sun/ts/tests/jms/core
    ret=0
    while [[ $ret == 0 ]]; do
        ant -Dtest.client=queuetests/QueueTests.java -Dtest=temporaryQueueNotConsumableTest_from_standalone runclient
        ret=$?
    done

# TODO:

* CI optimizations
  * cache the gradle install between runs
  * use --project-cache-dir --no-daemon --refresh-dependencies gradle options
