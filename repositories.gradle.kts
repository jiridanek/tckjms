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
                project.hasProperty("uberJar") -> Unit
                project.hasProperty("upstream") -> Unit
                project.hasProperty("local") -> mavenLocal()
                else -> {
                    if (!G.jmsClientVersion.contains("redhat")) {
                        throw InvalidUserDataException(
                            "define jmsClientVersion with `redhat` in it or use `-PuberJar` or `-Pupstream`")
                    }
                    when {
                        project.hasProperty("zip") -> flatDir {
                            // this would work for A-MQ6 zip distribution, if we want to go this route
                            dir(properties["distDir"] as String)
                        }
                        project.hasProperty("system") -> {
                            maven {
                                setUrl("file:///usr/share/java/maven-repo")
                            }
                        }
                        project.hasProperty("release") -> {
                            // see https://access.redhat.com/documentation/en/red-hat-jboss-a-mq/6.3/paged/client-connectivity-guide/chapter-1-introduction
                            maven {
                                // redhat-ga-repository
                                setUrl("https://maven.repository.redhat.com/ga")
                            }
                            maven {
                                // jboss-public
                                setUrl("https://repository.jboss.org/nexus/content/groups/public")
                            }
                            // for A-MQ 6.2.1, see https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_A-MQ/6.2/html/Client_Connectivity_Guide/Build-GenerateMaven.html
                            maven {
                                // fusesource
                                setUrl("http://repo.fusesource.com/nexus/content/groups/public/")
                                isAllowInsecureProtocol = true
                            }
                        }
                        project.hasProperty("candidate") -> {
                            maven {
                                // redhat-ea-repository
                                setUrl("https://maven.repository.redhat.com/earlyaccess/all")
                            }
                            maven {
                                // jboss-staging
                                setUrl("https://origin-repository.jboss.org/nexus/content/groups/m2-proxy/")
                            }
                            // for A-MQ 6.2.1
                            maven {
                                // fusesource.snapshot
                                setUrl("http://repo.fusesource.com/nexus/content/groups/public-snapshots/")
                                isAllowInsecureProtocol = true
                            }
                        }
                        project.hasProperty("repo-path") -> {
                            maven {
                                url = uri(project.property("repo-path") as String)
                                isAllowInsecureProtocol = true
                            }
                        }
                        else -> throw InvalidUserDataException("define one of `-Pupstream`, `-Plocal`, `-Pzip`, `-Psystem`, `-Prelease`, `-Pcandidate`, `-Prepo-path")
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
