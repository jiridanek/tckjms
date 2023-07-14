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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import java.io.File
import java.nio.file.Paths

/**
 * Hack, makes the Project instance effectively global.
 */
object P {
    /**
     * p is injected from the build script
     */
    lateinit var p: Project
}

open class ProjectDelegate {
    fun findProperty(p0: String): Any? = P.p.findProperty(p0)

    val properties: MutableMap<String, *>
        get() = P.p.properties

    val buildDir: File
        get() = P.p.buildDir

    val projectDir: File
        get() = P.p.projectDir
}

data class TCK(val url: String, val zip: String, val checksum: String, val patch: String) {}

/**
 * Global variables, allows access across build script files
 */
object G : ProjectDelegate() {
    const val JAVAX_TCK_VERSION = "2.0.0"
    const val JAVAX_TCK_ZIP = "jakarta-messaging-tck-$JAVAX_TCK_VERSION.zip"
    const val JAVAX_TCK_URL = "https://download.eclipse.org/jakartaee/messaging/2.0/$JAVAX_TCK_ZIP"
    val JAVAX_TCK = TCK(JAVAX_TCK_URL, JAVAX_TCK_ZIP,
        "a7a30ac36cc0b44e28706d56993e4fed113247f38c78726e285f7a9e9dd050f1eab552b37297fe07ba29dd3ddebae8d5",
        "jmstck_config.javax.patch")

    const val JAKARTA_TCK_VERSION = "3.1.0"
    const val JAKARTA_TCK_ZIP = "jakarta-messaging-tck-$JAKARTA_TCK_VERSION.zip"
    const val JAKARTA_TCK_URL = "https://download.eclipse.org/jakartaee/messaging/3.1/$JAKARTA_TCK_ZIP"
    val JAKARTA_TCK = TCK(JAKARTA_TCK_URL, JAKARTA_TCK_ZIP,
        "31b084c9b3e1d5be69c44b3d50d6e5fc604e14afe7a113cee3a47618cb9b6efb9460378ff70a61cb47b90c5c667bb021",
        "jmstck_config.jakarta.patch")

    val TS_HOME = buildDir.resolve("messaging-tck")
    lateinit var jmsClientConfiguration: Configuration

    var err = 0

    val tckZip: TCK
        get() {
            if (properties.containsKey("jmsClientVersion")) {
                val jmsClientVersion = properties.get("jmsClientVersion") as String
                when {
                    jmsClientVersion.startsWith("1.") -> return JAVAX_TCK
                    jmsClientVersion.startsWith("2.") -> return JAKARTA_TCK
                    else -> throw InvalidUserDataException("jmsClientVersion must be `1.` or `2.`")
                }
            }

            if (properties.containsKey("uberJar")) {
                val uberJarPath = properties.get("uberJar") as String
                val uberJar = Paths.get(projectDir.toString()).resolve(Paths.get(uberJarPath))
                    .toRealPath().fileName.toString()
                    .removePrefix("cli-qpid-jms-1.2.2-SNAPSHOT-")
                when {
                    uberJar.startsWith("1.") -> return JAVAX_TCK
                    uberJar.startsWith("2.") -> return JAKARTA_TCK
                    else -> throw InvalidUserDataException("uberJar messaging library version part must start with `1.` or `2.`, was $uberJar")
                }
            }

            throw InvalidUserDataException("could not figure out tckZip variant JAVAX/JAKARTA")
        }

    val jmsClientVersion: String
        get() = properties.getOrElse("jmsClientVersion", { "+" }) as String  // + means LATEST; only for upstream

    val jmsVersion: JmsCertVersion
        get() = when (findProperty("jmsVersion")) {
            "1.1" -> JmsCertVersion.JmsCert11
            "2.0" -> JmsCertVersion.JmsCert20
            else -> throw InvalidUserDataException("jmsVersion must be `1.1` or `2.0`")
        }

    val jmsClient: Client
        get() = when (findProperty("jmsClient")) {
            "qpid-jms" -> QpidJmsClient()
            "amqps-qpid-jms" -> AmqpsQpidJmsClient()
            "activemq-client" -> ActiveMQClient()
            "artemis-jms-client" -> ArtemisCoreJmsClient()
            else -> throw InvalidUserDataException("jmsClient must be `qpid-jms`, `activemq-client`, `amqps-qpid-jms` or `artemis-jms-client`")
        }


    val jmsBroker: Broker
        get() = when (findProperty("jmsBroker")) {
            "activemq-artemis" -> ArtemisBroker()
            "activemq" -> ActiveMqBroker()
            else -> throw InvalidUserDataException("jmsBroker must be `activemq-artemis` or `activemq`, was ${findProperty("jmsBroker")}")
        }

    val host: String
        get() = properties.getOrElse("host", { "localhost" }) as String

    val port: String?
        get() = findProperty("port") as String?  // individual clients have sensible defaults
}

/**
 * Global variables that hold Task instances
 */
object T {
    lateinit var configureRepositories: Task
}
