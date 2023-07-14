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

import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import groovy.util.ScriptException
import org.apache.tools.ant.filters.ReplaceTokens
import org.apache.tools.ant.taskdefs.Patch
import org.apache.tools.ant.taskdefs.condition.Os

P.p = project
G.jmsClientConfiguration = configurations.create("jmsClient")

// internal Gradle stuff

plugins {
    `kotlin-dsl`
    id("de.undercouch.download") version "5.4.0"
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

apply {
    from("destinations.gradle.kts")
    from("repositories.gradle.kts")
    from("logging.gradle.kts")
}

tasks {

    // Low level build tasks (see further below for high level tasks)

    val downloadAntTask by registering(Download::class) {
        src("https://mirror.dkm.cz/apache//ant/binaries/apache-ant-1.10.12-bin.zip")
        dest(buildDir.resolve("apache-ant-1.10.11-bin.zip"))
        overwrite(true)
        onlyIfNewer(true)
    }

    val unzipAntTask by registering(Copy::class) {
        dependsOn(downloadAntTask)
        inputs.file(downloadAntTask.get().outputFiles.first())
        outputs.dir(G.TS_HOME)
        from(zipTree(downloadAntTask.get().outputFiles.first()))
        into(buildDir)
    }

    val downloadTckTask by registering(Download::class) {
        src(G.tckZip.url)
        dest(buildDir.resolve(G.tckZip.zip))
        overwrite(false)
    }

    val verifyTckTask by registering(Verify::class) {
        dependsOn(unzipAntTask)
        val f = downloadTckTask.get().outputFiles.first()
        inputs.file(f)
        src(f)
        checksum(G.tckZip.checksum)
        algorithm("SHA-384")
    }

    downloadTckTask {
        finalizedBy(verifyTckTask)
    }

    /**
     * Creates directory 'messaging-tck' in buildDir
     */
    val unzipTckTask by registering(Copy::class) {
        dependsOn(downloadTckTask, verifyTckTask, unzipAntTask)
        inputs.file(downloadTckTask.get().outputFiles.first())
        outputs.dir(G.TS_HOME)
        from(zipTree(downloadTckTask.get().outputFiles.first()))
        into(buildDir)
    }

    /**
     * see https://ant.apache.org/manual/Tasks/patch.html
     */
    fun applyPatch(directory: File, patch: File) {
        Patch().apply {
            setPatchfile(patch)
            setDir(directory)
            setStrip(1)  // gets rid of the a/ b/ prefixes
            execute()
        }
    }

    val patchTckConfigTask by registering {
        dependsOn(unzipTckTask, T.configureRepositories)
        val resources = projectDir.resolve("src/main/resources")
        val patchFileName = G.tckZip.patch

        doLast {
            // classpath for running client and for TCK compilation
            val jars = G.jmsClientConfiguration.resolve().joinToString(separator = ":")  // magic happens here
            copy {
                from(resources.resolve(patchFileName))
                into(temporaryDir)
                filter(
                    mapOf("tokens" to mapOf(
                        "user" to G.jmsBroker.user,
                        "password" to G.jmsBroker.pass,
                        "client_libs" to jars,
                        "jmstck" to G.TS_HOME.toString())),
                    ReplaceTokens::class.java)
            }
            applyPatch(G.TS_HOME, temporaryDir.resolve(patchFileName))
        }
    }

    /**
     * Returns a map for variable replacement in the JMS TCK .properties file
     *
     * This is be flexible enough to accommodate configs that depend on both client _and_ broker:
     * pull data from the respective objects and mash them together
     */
    fun getConfigurationProperties(broker: Broker, client: Client): Map<String, String> {
        val connectionString = client.connectionString(broker)
        val durableConnectionString = client.durableConnectionString(broker)

        return mapOf(
            "client" to client::class.simpleName as String,
            "java_naming_factory_initial" to client.javaNamingFactoryInitial,
            "clientJndiOpts" to client.jndiOpts,
            "clientConnectionFactory" to client.connectionFactory,
            "MyConnectionFactory" to connectionString,
            "MyQueueConnectionFactory" to connectionString,
            "MyTopicConnectionFactory" to connectionString,
            "DURABLE_SUB_CONNECTION_FACTORY" to durableConnectionString,
            "queue_prefix" to if (client.needsQueueTopicPrefixes) broker.queue_prefix else "",
            "topic_prefix" to if (client.needsQueueTopicPrefixes) broker.topic_prefix else ""
        )
    }

    val configureTckTask by registering {
        dependsOn(unzipTckTask)
        val resources = projectDir.resolve("src/main/resources")
        val jndiFileDir = G.TS_HOME.resolve("classes")
        doLast {
            val properties = getConfigurationProperties(broker = G.jmsBroker, client = G.jmsClient)
            copy {
                from(resources.resolve("jndi.properties"))
                into(jndiFileDir)
                expand(properties)
            }
            copy {
                from(resources.resolve("log4j.properties"))
                into(jndiFileDir)
            }
        }
    }

    val patchTckTestsTask by registering {
//        doLast {
//            for (file in G.jmsVersion.patches) {
//                applyPatch(buildDir, patch = file)
//            }
//        }
    }

    val appendTckExcludes by registering {
        val excludeFile = G.TS_HOME.resolve("bin/ts.jtx")
        val excludes = G.jmsVersion.excludes + G.jmsClient.excludes
        doLast {
            excludeFile.appendText(excludes.joinToString("\n"))
        }
    }

    // helper tasks to run ant
    val antLauncherScript = when {
        Os.isFamily(Os.FAMILY_UNIX) -> buildDir.resolve("apache-ant-1.10.12/bin/ant")
        Os.isFamily(Os.FAMILY_WINDOWS) -> buildDir.resolve("apache-ant-1.10.12/bin/ant.bat")
        else -> throw ScriptException("Running on unsupported platform, neither Unix nor Windows")
    }

    val compileTckTask by registering {
        dependsOn(patchTckTestsTask, patchTckConfigTask, configureTckTask, unzipAntTask)
//        if (JavaVersion.current().isJava11Compatible) {
//            println("Skipping TCK compilation on Java 11+")
//        } else {
//            doLast {
//                exec {
//                    val output = buildDir.resolve("patched_clients_compilation.log").outputStream()
//                    executable(antLauncherScript)
//                    args("build.all")
//                    workingDir(G.TS_HOME.resolve("src/com/sun/ts/tests/jms"))
//                    standardOutput = output
//                    errorOutput = output
//                }
////            }
//        }
    }


    // high-level tasks to drive the build process

    val fetchTckTask by registering {
        dependsOn(downloadTckTask, verifyTckTask, unzipTckTask)
    }

    // not a good name, it applies patches and does config based on arguments
    val patchTckTask by registering {
        dependsOn(patchTckConfigTask, patchTckTestsTask)
        finalizedBy(appendTckExcludes)
    }

    // the main task

    val runTckTask by registering {
        dependsOn(fetchTckTask, patchTckTask, compileTckTask, unzipAntTask)
        doLast {
            exec {
                executable(antLauncherScript)
                args("runclient")
                workingDir(G.jmsVersion.testDir)
                standardOutput = buildDir.resolve("runclient.log").outputStream()
                errorOutput = buildDir.resolve("runclient.err").outputStream()
            }
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.BIN
    gradleVersion = "latest"
}
