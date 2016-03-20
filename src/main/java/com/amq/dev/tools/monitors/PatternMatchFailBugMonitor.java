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
package com.amq.dev.tools.monitors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.amq.dev.tools.Utils;

public class PatternMatchFailBugMonitor extends RunMonitor {

   boolean result = true;

   List<Pattern> patterns = new ArrayList<>();

   private static Logger logger = Logger.getLogger("BugSearch");

   public PatternMatchFailBugMonitor() {
      try {
         loadPatterns();
      }
      catch (Exception e) {
         throw new RuntimeException("Could not load patterns config");
      }
   }

   @Override
   public boolean getResult() {
      return result;
   }

   @Override
   public void run() {
      String line = null;

      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(process.getInputStream());

      try (BufferedReader reader = new BufferedReader(isr)) {
         while ((line = reader.readLine()) != null) {
            logger.fine(line);
            for (Pattern pattern : patterns) {
               if (pattern.eval(line)) {
                  result = pattern.isPass();
                  process.destroy();
                  return;
               };
            }
         }
      }
      catch (IOException e) {
         e.printStackTrace();
      }
      finally {
         try {
            isr.close();
            is.close();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private void loadPatterns() throws IOException {
      Properties p = Utils.getProperties("/conf/patterns.properties");
      for (String key : p.stringPropertyNames()) {
         patterns.add(new Pattern(p.getProperty(key)));
      }
   }

   private class Pattern {

      private String pattern;

      private int maxOccurances;

      private boolean pass;

      private int occurances = 0;

      public Pattern(String pattern) {
         String[] p = pattern.split(",");
         this.pattern = p[0];
         this.maxOccurances = Integer.parseInt(p[1]);
         this.pass = Boolean.parseBoolean(p[2]);
      }

      public boolean isPass() {
         return pass;
      }

      /**
       * Returns true if this pattern has evaluated as true.  I.e. the numner of patters has reached the max occurances
       * and the Pattern is matched.
       * @param line
       * @return
       */
      public boolean eval(String line) {
         if (line.contains(pattern)) {
            occurances++;
            return occurances >= maxOccurances;
         }
         return false;
      }
   }
}
