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
package com.amq.dev.tools;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amq.dev.tools.monitors.LogStreamMonitor;
import com.amq.dev.tools.monitors.PatternMatchFailBugMonitor;
import com.amq.dev.tools.monitors.RunMonitor;
import com.amq.dev.tools.search.Binary;
import com.amq.dev.tools.search.ReverseLinear;
import com.amq.dev.tools.search.SearchAlgorithm;

public class BugSearch {

   public static final String PROPERTIES_FILE = "conf/config.properties";

   public static final String ENVIRONMENT_FILE = "conf/environment.properties";

   // Configuration Property Keys
   public static final String HOME_DIR_KEY = "DEV_TOOLS_HOME";

   public static final String WORKFLOW_KEY = "workflow";

   public static final String TOP_COMMIT_KEY = "topCommit";

   public static final String SEARCH_SIZE_KEY = "searchSize";

   public static final String SEARCH_ALGORITHM_KEY = "searchAlgorithm";

   public static final String SOURCE_DIR_KEY = "srcDirectory";

   public static final String TEST_ITERATIONS_KEY = "testIterations";

   // Environment Keys (for writing configuration properties to script environment
   public static final String SEARCH_VALUE_ENV_KEY = "SEARCH_VALUE";

   public static final String START_COMMIT_ENV_KEY = "START_COMMIT";

   public static final String SRC_DIR_ENV_KEY = "SRC_DIR";


   // Algorithm Names
   public static final String BINARY_SEARCH_ALGORITHM_NAME = "Binary";

   public static final String REVERSE_LINEAR_ALGORITHM_NAME = "ReverseLinear";

   public static final String LOG_LEVEL_KEY = "logLevel";

   // Instance Variables
   static String homeDir;

   private String[] workflow;

   private String topCommit;

   private int searchSize;

   private int testIterations;

   private String searchAlgorithmName;

   private Properties properties;

   private Properties environment;

   private SearchAlgorithm searchAlgorithm;

   protected String lastGoodCommit = "none";

   private static Logger logger = Logger.getLogger("BugSearch");

   public static void main(String[] args) {
      try {

         homeDir = System.getenv(HOME_DIR_KEY);
         if (homeDir == null) {
            throw new RuntimeException(HOME_DIR_KEY + " not set");
         }

         BugSearch s = new BugSearch();
         s.search();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public BugSearch() {
      try {
         loadEnvironment();
         loadProperties();
         search();
      }
      catch (IOException e) {
         throw new RuntimeException("Unable to load configuration", e);
      }
      catch (Exception e) {
         throw new RuntimeException("Error occurred whilst running search", e);
      }
   }

   private void search() throws Exception {
      logger.info("Starting Search...");

      int nextValue = searchAlgorithm.nextValue();
      while(searchAlgorithm.nextValue() >= 0) {
         boolean result = runTests(nextValue);
         searchAlgorithm.result(result, nextValue);
         if (result) {
            lastGoodCommit = topCommit + " ~" + nextValue;
         }
      }
   }

   private void setSearchAlgorithm() {
      if (searchAlgorithmName.equals(BINARY_SEARCH_ALGORITHM_NAME)) {
         searchAlgorithm = new Binary();
      }
      else if (searchAlgorithmName.equals(REVERSE_LINEAR_ALGORITHM_NAME)) {
         searchAlgorithm = new ReverseLinear();
      }
      else {
         throw new RuntimeException("Search Algorithm Not Recognised: " + searchAlgorithmName);
      }
      searchAlgorithm.setSearchSize(searchSize);
   }

   public boolean runTests(int searchValue) throws Exception {
      for (int i = 0; i < testIterations; i++) {
         if (!runWorkflow(searchValue, i))
            logger.info("Test Failed: HEAD~" + searchValue + " Iteration: " + (testIterations + 1));
            return false;
      }
      logger.info("Test Passed after " + (testIterations + 1) + "runs Commit=HEAD~" + searchValue);
      return true;
   }

   private boolean runWorkflow(int searchValue, int iteration) throws Exception {
      RunMonitor monitor;
      boolean result = true;
      for (int i = 0; i < workflow.length; i++) {
         String step = workflow[i];
         environment.setProperty(SEARCH_VALUE_ENV_KEY, "" + searchValue);

         logger.info("Running Step: " + step + " on HEAD~" + searchValue + " Iteration: " + (iteration + 1));

         Process process = Utils.executeStep(homeDir, environment, step);
         if (step.equals("Run")) {
            monitor = new PatternMatchFailBugMonitor();
         }
         if (step.equals("Build") && iteration > 0) {
            //We don't need to rebuild, since we are on the same commit
            continue;
         }
         else {
            monitor = new LogStreamMonitor();
         }

         monitor.setProcess(process);
         monitor.setProperties(properties);
         result = result && runMonitor(monitor);
      }
      return result;
   }

   private boolean runMonitor(RunMonitor runMonitor) throws InterruptedException {
      Thread t = new Thread(runMonitor);
      t.start();
      runMonitor.getProcess().waitFor();
      t.join();
      return runMonitor.getResult();
   }

   private void loadProperties() throws IOException {
      properties = Utils.getProperties(PROPERTIES_FILE);
      workflow = properties.getProperty(WORKFLOW_KEY).split(",");
      topCommit = properties.getProperty(TOP_COMMIT_KEY);
      searchSize = Integer.parseInt(properties.getProperty(SEARCH_SIZE_KEY));
      searchAlgorithmName = properties.getProperty(SEARCH_ALGORITHM_KEY);
      testIterations = Integer.parseInt(properties.getProperty(TEST_ITERATIONS_KEY));
      environment.setProperty(START_COMMIT_ENV_KEY, topCommit);
      environment.setProperty(SRC_DIR_ENV_KEY, properties.getProperty(SOURCE_DIR_KEY));

      setLogLevel(properties.getProperty(LOG_LEVEL_KEY));
      setSearchAlgorithm();

      logger.info("Configuration Loaded...");
   }

   private void setLogLevel(String level) {
      Level l = Level.parse(level);

      logger.setLevel(l);
      for(Handler h : logger.getParent().getHandlers()){
         if(h instanceof ConsoleHandler){
            h.setLevel(l);
         }
      }
   }
   private void loadEnvironment() throws IOException {
      environment = Utils.getProperties(ENVIRONMENT_FILE);
      logger.info("Environment Loaded...");
   }
}
