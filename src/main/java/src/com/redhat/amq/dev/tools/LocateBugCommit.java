/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package src.com.redhat.amq.dev.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LocateBugCommit {

   public static final String srcDir = "/home/mtaylor/dev/rh-messaging/jboss-activemq-artemis/";

   public static final String workingDir = "~/dev/eap/eap/eap-tests-hornetq/jboss-hornetq-testsuite";

   public static final String buildCmd = "mvn clean install package -DskipTests=true";

   public static final String preRunTestCmd =   "cd " + workingDir + ";" +
      "rm -rf ../scripts/*journal-*;" +
      "for i in `ps ax | grep standalone | grep -o '^\\S\\+'`; do kill -9 $i; done;" +
      "cd /home/mtaylor/dev/eap/eap/eap-tests-hornetq/scripts/;" +
      "./replace-eap.sh " + srcDir + " /home/mtaylor/dev/eap/eap/eap-tests-hornetq/scripts/server1/jboss-eap/;" +
      "./replace-eap.sh " + srcDir + " /home/mtaylor/dev/eap/eap/eap-tests-hornetq/scripts/server2/jboss-eap/;";


   public static final String runTestCmd = "source /home/mtaylor/.bashrc;" +
      "cd " + workingDir  + ";" +
      "ulimit -u 62624;" +
      "export WORKSPACE=/home/mtaylor/dev/eap/eap/eap-tests-hornetq/scripts/;" +
      "export JBOSS_HOME_1=$WORKSPACE/server1/jboss-eap;" +
      "export JBOSS_HOME_2=$WORKSPACE/server2/jboss-eap;" +
      "export JBOSS_HOME_3=$WORKSPACE/server3/jboss-eap;" +
      "export JBOSS_HOME_4=$WORKSPACE/server4/jboss-eap;" +
      "export JOURNAL_DIRECTORY_A=$WORKSPACE/journal-A;" +
      "export JOURNAL_DIRECTORY_B=$WORKSPACE/journal-B;" +
      "mvn clean test -Dtest=ReplicatedColocatedClusterFailoverTestCase#testFailbackClientAckTopic -DfailIfNoTests=false -Deap=7x | tee log";


   public static final String postRunTestCmd =  "cd " + workingDir + ";" +
      "for i in `ps ax | grep standalone | grep -o '^\\S\\+'`; do kill -9 $i; done;";

   public static final String startCommit = "82b680d0277414ed321b909454a9ac926bf01ff9";

   public static final int endCommit = 8;

   // If the bug is intermittent, you might want to run the same test several times on the same commit.
   public static final int rerun = 5;

   private Runtime runtime;

   public void main() throws Exception {
      runtime = Runtime.getRuntime();

      int topCommit = 0;
      int bottomCommit = endCommit;
      int nextCommit = 0;
      boolean succeeded = false;
      String lastGoodCommit = "none";
      int runTimes = 0;

      while(topCommit <= bottomCommit) {

         // Only move to the next commit if the test was successful *rerun times or test failed.
         if (!succeeded || runTimes >= rerun) {
            // We're doing binary search, false = moves towards first commit, true move towards last
            if (succeeded) {
               bottomCommit = nextCommit;
               nextCommit = (topCommit + bottomCommit) / 2;
            }
            else {
               topCommit = nextCommit;
               nextCommit = (bottomCommit + topCommit) / 2;
            }

            checkoutCommit(nextCommit);
            //exec("cd " + srcDir + ";" + buildCmd, "build src", true);
            runTimes = 0;
         }

         exec(preRunTestCmd, " pre run step ", true);
         succeeded = run();

         if (succeeded && runTimes == rerun) {
            lastGoodCommit = startCommit + " ~ " + nextCommit;
         }

         exec(postRunTestCmd, " post run step ", true);
         runTimes++;

         exec("cd " + workingDir + "; mkdir -p " + nextCommit + "; mv log " + nextCommit + "/" + runTimes, "move logs", true);
      }

      System.out.println("LAST GOOD COMMIT: " + lastGoodCommit);
   }

   private boolean run() throws IOException, InterruptedException {

      // If the process exits before this then we assume failure
      long maxTime = 500000;
      long timeNow = System.currentTimeMillis();

      Process p = runtime.exec(new String[] {"/bin/bash", "-c", runTestCmd});

      InputStream in = p.getInputStream();
      final InputStream err = p.getErrorStream();

      final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String line;
      int count = 0;
      int maxCount = 40;
      String pattern = "AMQ212034: There are more than one servers on the network broadcasting the same node id.";

      Runnable r = new Runnable() {
         @Override
         public void run() {
            String line;
            try {
               while ((line = reader.readLine()) != null) {
                  System.err.println(line);
               }
            }
            catch (IOException e) {
               e.printStackTrace();
            }
         }
      };
      Thread t = new Thread(r);
      t.start();

      while ((line = reader.readLine()) != null) {
         System.out.println(line);
         if(line.contains(pattern)) {
            count++;
            if (System.currentTimeMillis() > (timeNow + maxTime)) {
               p.destroyForcibly();
               return false;
            }
            if (count > maxCount) {
               p.destroyForcibly();
               return false;
            }
         }
      }

      p.waitFor();

      return true;
   }

   private void checkoutCommit(int beforeHead) throws Exception {
      String cmd = "cd " + srcDir + "; git checkout " + startCommit + "; git checkout HEAD~" + beforeHead;
      exec(cmd, "git checkout", true);
   }

   private void exec(String cmd, String step, boolean block) throws Exception {
      System.out.println("##################" + step +  "##################");
      System.out.println("Exec: " + cmd);
      printInputStream(blockOnProcess(cmd, true, true));
      System.out.println("##################################################");
   }

   private InputStream blockOnProcess(String cmd, boolean wait, boolean print) throws Exception {
      Process p = runtime.exec(new String[] {"/bin/bash", "-c", cmd});
      if (print) {
         printInputStream(p.getInputStream());
         printInputStream(p.getErrorStream());
      }

      if (wait) {
         if (p.waitFor() == -1) {
            throw new RuntimeException("Error executing cmd: " + cmd);
         }
      }
      return p.getInputStream();
   }

   private void printInputStream(InputStream stream) throws IOException, InterruptedException {
      final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      String line = null;
      while ((line = reader.readLine()) != null) {
         System.out.println(line);
      }
   }
}
