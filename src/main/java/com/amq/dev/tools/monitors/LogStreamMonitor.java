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
import java.util.logging.Logger;

public class LogStreamMonitor extends RunMonitor {

   @Override
   public boolean getResult() {
      return true;
   }

   private static Logger logger = Logger.getLogger("BugSearch");

   @Override
   public void run() {
      String line = null;

      InputStream is = process.getInputStream();
      InputStreamReader isr = new InputStreamReader(process.getInputStream());

      try (BufferedReader reader = new BufferedReader(isr)) {
         while ((line = reader.readLine()) != null) {
            logger.fine(line);
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
}
