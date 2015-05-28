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
import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.data.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import com.endpoint.lg.support.message.Scene;
import com.endpoint.lg.support.message.Window;

/**
 * An activity for handling director scene changes.
 * 
 * @author Matt Vollrath <matt@endpoint.com>
 */
public class DirectorBridgeActivity extends BaseRoutableRosActivity {
  public static final String CONFIG_MASTER_URI = "lg.master.api.uri";
  public static final String CONFIG_GROUP_EARTH = "lg.director.group.earth";
  public static final String CONFIG_GROUP_STREETVIEW = "lg.director.group.streetview";
  public static final String CONFIG_GROUP_PANOVIEWER = "lg.director.group.panoviewer";

  private MasterApi master;

  private String masterUri;
  private String groupEarth;
  private String groupStreetview;
  private String groupPanoviewer;

  private void activateGroup(String name) {
    try {
      master.activateLiveActivityGroup(name);
    } catch (SimpleInteractiveSpacesException e) {
      getLog().error("Could not activate a live activity group");
      getLog().error(e.getMessage());
    }
  }

  private void deactivateGroup(String name) {
    try {
      master.deactivateLiveActivityGroup(name);
    } catch (SimpleInteractiveSpacesException e) {
      getLog().error("Could not deactivate a live activity group");
      getLog().error(e.getMessage());
    }
  }

  /**
   * Handles a scene message.
   * 
   * @param message
   *          A scene from the director.
   */
  private void handleSceneMessage(String message) {
    Scene scene;

    try {
      scene = Scene.fromJson(message);
    } catch (IOException e) {
      getLog().error("Error while parsing scene message");
      getLog().error(e.getMessage());
      return;
    }

    getLog().info("Handling scene: " + scene.name);

    String app = "earth"; // default to Earth
    
    for (Window w : scene.windows) {
      if (w.activity.equals("streetview")) {
        app = "streetview";
        break;
      } else if (w.activity.equals("pano")) {
        app = "pano";
        break;
      }
    }
    
    if (app.equals("earth")) {
      getLog().info("Earth scene");
      activateGroup(groupEarth);
      deactivateGroup(groupStreetview);
      deactivateGroup(groupPanoviewer);
    } else if (app.equals("streetview")) {
      getLog().info("Street View scene");
      activateGroup(groupStreetview);
      deactivateGroup(groupEarth);
      deactivateGroup(groupPanoviewer);
    } else if (app.equals("pano")) {
      getLog().info("Pano Viewer scene");
      activateGroup(groupPanoviewer);
      deactivateGroup(groupStreetview);
      deactivateGroup(groupEarth);
    }
  }

  @Override
  public void onActivitySetup() {
    masterUri = getConfiguration().getPropertyString(CONFIG_MASTER_URI);
    groupEarth = getConfiguration().getPropertyString(CONFIG_GROUP_EARTH);
    groupStreetview = getConfiguration().getPropertyString(
        CONFIG_GROUP_STREETVIEW);
    groupPanoviewer = getConfiguration().getPropertyString(
        CONFIG_GROUP_PANOVIEWER);

    master = new MasterApi(getLog(), masterUri);
  }

  @Override
  public void onNewInputJson(String channelName, Map<String, Object> message) {
    getLog().info("Got a message on " + channelName);

    String json = JsonMapper.INSTANCE.toString(message);
    getLog().info(json);

    if (channelName.equals("scene")) {
      handleSceneMessage(json);
    }
  }
}
