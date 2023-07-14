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

import java.io.File

// Configuration objects

enum class JmsCertVersion(val testDir: File, val patches: List<File>, val excludes: List<String> = emptyList()) {
    JmsCert20(
            testDir = G.TS_HOME.resolve("src/com/sun/ts/tests/jms"),
            patches = listOf(G.projectDir.resolve("tck20_testDateFormatPermanentFix.patch")),
            excludes = listOf(
                // ENTMQBR-1542 ENTMQCL-524: Exclude TopicRequestorException test
                "com/sun/ts/tests/jms/core/appclient/topictests/TopicTests.java#topicRequestorExceptionTests_from_standalone",
            )
    ),
    JmsCert11(
            testDir = G.TS_HOME.resolve("src/com/sun/ts/tests/jms/core"),
            patches = listOf(
                    G.projectDir.resolve("tck20_testDateFormatPermanentFix.patch"),
                    G.projectDir.resolve("tck20_jmsVersionPermanentFix.patch")
                    // TODO(jdanek): why is this patch disabled? no longer necessary? always?
                    // projectDir.resolve("tck20_addingSleepBeforeCommitWorkaroundFix.patch")
            ),
            excludes = listOf(
                    // Excluding some of the tests that are not in JMS 1.1 suite
                    "com/sun/ts/tests/jms/core/queueMsgProperties/QueuePropertyTests.java#msgPropertiesQTest_from_standalone",
                    "com/sun/ts/tests/jms/core/topicMsgProperties/TopicPropertyTests.java#msgPropertiesTopicTest_from_standalone",
                    // ENTMQBR-1542 ENTMQCL-524: Exclude TopicRequestorException test
                    "com/sun/ts/tests/jms/core/appclient/topictests/TopicTests.java#topicRequestorExceptionTests_from_standalone",
                    // TODO(jdanek): why are these tests not disabled? no longer necessary? always?
                    // "com/sun/ts/tests/jms/core/topictests/TopicTests.java#durableSubscriberTopicNoLocalTest2_from_standalone"
            )
    )
}
