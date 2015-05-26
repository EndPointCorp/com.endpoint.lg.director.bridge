/*
 * Copyright (C) 2015 End Point Corporation
 * Copyright (C) 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.endpoint.lg.director.bridge;

import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.util.data.json.JsonMapper;
import interactivespaces.util.data.json.JsonNavigator;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;

/**
 * An interface to the ugly Master API.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class MasterApi {
  public static final String COMMAND_ACTIVATE = "activate";
  public static final String COMMAND_DEACTIVATE = "deactivate";
  public static final String COMMAND_LIST = "all";

  public static final String ENTITY_LIVEACTIVITYGROUP = "liveactivitygroup";

  public static final String RESPONSE_FIELD_DATA = "data";
  public static final String RESPONSE_FIELD_ID = "id";
  public static final String RESPONSE_FIELD_NAME = "name";

  public static final String REQUEST_EXT = ".json";
  public static final int ERR_NO_RESPONSE = -1;
  public static final int ERR_NOT_FOUND = -2;

  private JsonNavigator liveActivityGroupCache;
  private HttpClient client;
  private JsonMapper jsonMapper;

  private String masterUrl;
  private Log log;

  public MasterApi(Log log, String url) {
    masterUrl = url;
    this.log = log;
    client = new HttpClient();
    jsonMapper = new JsonMapper();
    liveActivityGroupCache = null;
  }

  private JsonNavigator sendRequest(String path) {
    String url = masterUrl + "/" + path;
    log.info("Hitting url: " + url);
    GetMethod method = new GetMethod(url);
    JsonNavigator responseJson = null;

    try {
      // Execute the method.
      int statusCode = client.executeMethod(method);

      if (statusCode != HttpStatus.SC_OK) {
        System.err.println("Method failed: " + method.getStatusLine());
      }

      // Read the response body.
      String responseBody = new String(method.getResponseBody());
      log.info(responseBody);
      responseJson = new JsonNavigator(jsonMapper.parseObject(responseBody));

    } catch (HttpException e) {
      System.err.println("Fatal protocol violation: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.err.println("Fatal transport error: " + e.getMessage());
      e.printStackTrace();
    } finally {
      // Release the connection.
      method.releaseConnection();
    }

    return responseJson;
  }

  private void fetchLiveActivityGroups() {
    String url = ENTITY_LIVEACTIVITYGROUP + "/" + COMMAND_LIST + REQUEST_EXT;
    
    JsonNavigator responseJson = sendRequest(url);
    if (responseJson.containsProperty(RESPONSE_FIELD_DATA)) {
      responseJson.down(RESPONSE_FIELD_DATA);
    } else {
      responseJson = null;
    }
    
    liveActivityGroupCache = responseJson;
  }

  private synchronized int lookupLiveActivityGroup(String name) {
    int id = ERR_NOT_FOUND;

    if (liveActivityGroupCache == null) {
      fetchLiveActivityGroups();
    }

    if (liveActivityGroupCache == null) {
      return ERR_NO_RESPONSE;
    }

    for (int i = 0; i < liveActivityGroupCache.getSize(); i++) {
      liveActivityGroupCache.down(i);
      if (liveActivityGroupCache.getString(RESPONSE_FIELD_NAME).equals(name)) {
        String s = liveActivityGroupCache.getString(RESPONSE_FIELD_ID);
        id = Integer.parseInt(s);
      }
      liveActivityGroupCache.up();

      if (id > 0) {
        break;
      }
    }

    return id;
  }

  private void manipulateLiveActivityGroup(String name, String command)
      throws SimpleInteractiveSpacesException {
    int id = lookupLiveActivityGroup(name);
    if (id < 0) {
      throw new SimpleInteractiveSpacesException("Master request failed for "
          + name + ", error " + id);
    }

    String url = ENTITY_LIVEACTIVITYGROUP + "/" + id + "/" + command
        + REQUEST_EXT;
    JsonNavigator response = sendRequest(url);
    if (response == null) {
      throw new SimpleInteractiveSpacesException(
          "No response from master request for " + name);
    }
  }

  /**
   * Activates a Live Activity Group by name.
   * 
   * @param name
   * @throws SimpleInteractiveSpacesException
   */
  public void activateLiveActivityGroup(String name)
      throws SimpleInteractiveSpacesException {
    manipulateLiveActivityGroup(name, COMMAND_ACTIVATE);
  }

  /**
   * Activates a Live Activity Group by name.
   * 
   * @param name
   * @throws SimpleInteractiveSpacesException
   */
  public void deactivateLiveActivityGroup(String name)
      throws SimpleInteractiveSpacesException {
    manipulateLiveActivityGroup(name, COMMAND_DEACTIVATE);
  }
}
