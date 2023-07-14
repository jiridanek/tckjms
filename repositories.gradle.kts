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

T.configureRepositories = task("configureRepositories") {
    doLast {
        repositories {
            mavenCentral()
            when {
                project.hasProperty("maven-central") -> Unit // artifact is in maven central repository
                project.hasProperty("local") -> mavenLocal() // enables local maven repository, useful for for SNAPSHOTs
                project.hasProperty("uberJar") -> Unit // adds the jar file to classpath, see below
                else -> {
                    if (!G.jmsClientVersion.contains("redhat")) {
                        throw InvalidUserDataException(
                            "define jmsClientVersion with `redhat` in it or use `-Pmaven-central`, `-Plocal` or `-PuberJar`")
                    }
                    when {
                        project.hasProperty("flatDirs") -> flatDir {
                            dirs((properties["flatDirs"] as String).split(";"))
                        }
                        project.hasProperty("repoDirs") -> {
                            for (repoDir in (properties["repoDirs"] as String).split(";")) {
                                maven {
                                    setUrl("file://$repoDir")
                                }
                            }
                        }
                        project.hasProperty("repoUrls") -> {
                            for (aUrl in (project.property("repoUrls") as String).split(";")) {
                                maven {
                                    url = uri(aUrl)
                                    isAllowInsecureProtocol = true
                                }
                            }
                        }
                        else -> throw InvalidUserDataException("define one of `-Pmaven-central`, `-Plocal`, `-PuberJar`, `-PflatDirs`, `-PrepoDirs`, `-PrepoUrls`")
                    }
                }
            }
        }

        dependencies {
            // add(jmsClientConfiguration.name, "org.slf4j:slf4j-api:+")  // latest upstream is 1.7.21
            add(G.jmsClientConfiguration.name, "org.slf4j:slf4j-log4j12:+")
            when {
                project.hasProperty("uberJar") -> {
                    add(G.jmsClientConfiguration.name, files(properties["uberJar"] as String))
                }
                else -> add(G.jmsClientConfiguration.name, G.jmsClient.artifact)
            }
        }
    }
}

// for debugging
task("printJmsClientConfiguration") {
    dependsOn(T.configureRepositories)
    doLast {
        for (d in G.jmsClientConfiguration.resolve()) {
            logger.error(d.toString())
        }
    }
}
