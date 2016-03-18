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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Utils {

   public static Properties getProperties(String fileName) throws IOException {
      Properties properties = new Properties();
      properties.load(new FileInputStream(BugSearch.homeDir + "/" + fileName));
      return properties;
   }

   public static Process executeStep(String homeDir, Properties env, String step) throws IOException {
      ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", homeDir + "/steps/" + step + ".sh");
      processBuilder.redirectErrorStream(true);
      for (String key : env.stringPropertyNames()) {
         processBuilder.environment().put(key, env.getProperty(key));
      }
      return processBuilder.start();
   }
}
