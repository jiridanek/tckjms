/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// helper tasks to create and delete destinations on broker

fun modifyDestination(action: String, amqxObject: String, destinationName: String) {
    val amqxJar = projectDir.resolve("../../clients/amqx.jar").absolutePath
    val opts = G.jmsBroker.amqxOpts

    val execution = exec {
        executable("java")
        args("-jar", amqxJar, amqxObject, "--action", action, "--name", destinationName, "--host", "${G.host}:1099", opts)
        isIgnoreExitValue = true
    }
    if (execution.exitValue != 0) {
        G.err++
    }
}

fun modifyDestinations(action: String) {
    val queues = listOf("MY_QUEUE", "MY_QUEUE2", "testQ0", "testQ1", "testQ2", "testQueue2", "Q2", "myQueue")
    val topics = listOf("MY_TOPIC", "MY_TOPIC2", "testT0", "testT1", "testT2", "myTopic")  // see MSGQE-1411 about myTopic
    for (queue in queues) {
        val prefix = G.jmsBroker.queue_prefix
        val amqxObject = G.jmsBroker.amqxQueueObject
        modifyDestination(action, amqxObject, prefix + queue)
    }
    for (topic in topics) {
        val prefix = G.jmsBroker.topic_prefix
        val amqxObject = G.jmsBroker.amqxTopicObject
        modifyDestination(action, amqxObject, prefix + topic)
    }
}

val addDestinationsTask = task("addDestinations") {
    doLast {
        modifyDestinations("add")
    }
}

val removeDestinationsTask = task("removeDestinations") {
    doLast {
        modifyDestinations("remove")
    }
}
