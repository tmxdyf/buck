/*
 * Copyright 2013-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.java;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.event.BuckEvent;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.BuildRuleEvent;
import com.facebook.buck.rules.CacheResult;
import com.facebook.buck.testutil.integration.DebuggableTemporaryFolder;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.TestDataHelper;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PrebuiltJarIntegrationTest {

  @Rule
  public DebuggableTemporaryFolder temp = new DebuggableTemporaryFolder();


  @Test
  public void testAbiKeyIsHashOfFileContents() throws IOException, InterruptedException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "prebuilt",
        temp);
    workspace.setUp();

    ProjectWorkspace.ProcessResult result = workspace.runBuckBuild("//:jar");
    result.assertSuccess();

    BuildRuleEvent.Finished finished = getRuleFinished(result.getCapturedEvents());
    assertEquals(CacheResult.MISS, finished.getCacheResult());

    result = workspace.runBuckBuild("//:jar");
    result.assertSuccess();
    finished = getRuleFinished(result.getCapturedEvents());
    assertEquals(CacheResult.LOCAL_KEY_UNCHANGED_HIT, finished.getCacheResult());

    // We expect the binary jar to have a different hash to the stub jar.
    File binaryJar = workspace.getFile("junit.jar");
    HashCode originalHash = Files.asByteSource(binaryJar).hash(Hashing.sha1());
    Path expectedOut =
        BuildTargets.getGenPath(BuildTarget.builder("//", "jar").build(), "%s-abi.jar");
    File abiJar = workspace.getFile(expectedOut.toString());
    HashCode abiHash = Files.asByteSource(abiJar).hash(Hashing.sha1());

    assertTrue(abiJar.exists());
    assertNotEquals(originalHash, abiHash);
  }

  private BuildRuleEvent.Finished getRuleFinished(List<BuckEvent> capturedEvents) {
    BuildRuleEvent.Finished finished = null;
    for (BuckEvent capturedEvent : capturedEvents) {
      if (!(capturedEvent instanceof BuildRuleEvent.Finished)) {
        continue;
      }
      finished = (BuildRuleEvent.Finished) capturedEvent;
    }
    assertNotNull(finished);
    return finished;
  }
}
