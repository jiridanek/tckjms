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

abstract class Client {
    abstract val javaNamingFactoryInitial: String
    abstract val protocol: String
    abstract val port: String
    abstract val needsQueueTopicPrefixes: Boolean
    abstract val artifact: String
    abstract val connectionFactory: String
    abstract val idProperty: String
    open val jndiOpts = ""
    open val excludes: List<String> = emptyList()

    abstract fun connectionString(broker: Broker): String

    abstract fun durableConnectionString(broker: Broker): String
}

open class QpidJmsClient : Client() {
    override val javaNamingFactoryInitial = "org.apache.qpid.jms.jndi.JmsInitialContextFactory"
    override val protocol = "amqp"
    override val port = "5672"
    override val needsQueueTopicPrefixes = true
    override val artifact: String  // because value of jmsClientVersion is not known at initialization time; it's null
        get() = "org.apache.qpid:qpid-jms-client:${G.jmsClientVersion}"
    override val connectionFactory = "connectionFactory"  // qpid-jms accepts lowercase f, artemis does not
    override val idProperty = "jms.clientID"

    override fun connectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$host:$port?jms.prefetchPolicy.all=0&jms.username=$user&jms.password=$pass"
    }

    override fun durableConnectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$host:$port?jms.clientID=cts&jms.prefetchPolicy.all=0&jms.username=$user&jms.password=$pass"
    }
}

class EnmasseQpidJmsClient : QpidJmsClient() {
    override val protocol = "amqps"
    override val connectionFactory = "connectionFactory"  // qpid-jms accepts lowercase f, artemis does not
    val properties = "&transport.trustAll=true&transport.verifyHost=false"

    override fun connectionString(broker: Broker): String = super.connectionString(broker) + properties

    override fun durableConnectionString(broker: Broker): String = super.durableConnectionString(broker) + properties
}

class ActiveMQClient : Client() {
    override val javaNamingFactoryInitial = "org.apache.activemq.jndi.ActiveMQInitialContextFactory"
    override val protocol = "tcp"
    override val port = "61616"
    override val needsQueueTopicPrefixes = false
    override val artifact: String
        get() = "org.apache.activemq:activemq-client:${G.jmsClientVersion}"
    override val connectionFactory = "connection"
    override val jndiOpts = "connectionFactoryNames=MyConnectionFactory, MyQueueConnectionFactory, MyTopicConnectionFactory, DURABLE_SUB_CONNECTION_FACTORY"
    override val idProperty = "jms.clientID"
    override val excludes = listOf(
        // https://issues.redhat.com/browse/ENTMQBR-1985 JMS TCK failures with Openwire protocol
        "com/sun/ts/tests/jms/core/queueMsgProperties/QueuePropertyTests.java#msgPropertyExistTest_from_standalone",
        "com/sun/ts/tests/jms/core/queueConnection/QueueConnectionTests.java#metaDataTests_from_standalone",
        "com/sun/ts/tests/jms/core/topicConnection/TopicConnectionTests.java#metaDataTests_from_standalone",
    )

    override fun connectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$user:$pass@$host:$port"
    }

    override fun durableConnectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$user:$pass@$host:$port?$idProperty=cts"
    }
}

class ArtemisCoreJmsClient : Client() {
    override val javaNamingFactoryInitial = "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
    override val protocol = "tcp"
    override val port = "61616"
    override val needsQueueTopicPrefixes = false
    override val artifact: String
        get() = "org.apache.activemq:artemis-jms-client:${G.jmsClientVersion}"
    override val connectionFactory = "connectionFactory"  // qpid-jms accepts lowercase f, artemis does not
    override val idProperty = "clientID"

    override fun connectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$host:$port?user=$user&password=$pass"
    }

    override fun durableConnectionString(broker: Broker): String {
        val user = broker.user
        val pass = broker.pass
        val host = G.host
        val port = G.port ?: port
        return "$protocol://$host:$port?user=$user&password=$pass&$idProperty=cts"
    }
}
