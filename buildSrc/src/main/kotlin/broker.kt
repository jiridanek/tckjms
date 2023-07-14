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

abstract class Broker {
    val user = "tckuser"  // user and password; Dtests creates that on the broker
    val pass = "tckuser"
    open val queue_prefix = ""
    open val topic_prefix = ""

    abstract val amqxQueueObject: String
    abstract val amqxTopicObject: String
    open val amqxOpts = ""
}

class AMQ6Broker : Broker() {
    override val amqxQueueObject = "queue"
    override val amqxTopicObject = "jmstopic"
    override val amqxOpts = "--broker-type activemq"
}

class AMQ7Broker : Broker() {
    override val amqxQueueObject = "queue"
    override val amqxTopicObject = "topic"
}
