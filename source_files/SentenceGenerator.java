
package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.*;
import java.net.*;
import java.io.*;
import org.json.*;

import android.util.Log;

@DesignerComponent(version = SentenceGenerator.VERSION, description = "Generates text using input seed text.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "appengine/src/com/google/appinventor/images/TODO.png")
@SimpleObject(external = true)
public class SentenceGenerator extends AndroidNonvisibleComponent implements Component {
  public static final int VERSION = 1;
  private ComponentContainer container;
  private final boolean DEBUG = true;
  private final String LOG_TAG = "SentenceGenerator";
  private final String baseURLString = "http://appinventor-alexa.csail.mit.edu:1234/";

  // example variable for extension
  private String exampleVar;

  // defaults:
  public static final String DEFAULT_EXAMPLE_VAR_VALUE = "default";

  /**
   * Constructor creates a new extension object with default values.
   */
  public SentenceGenerator(ComponentContainer container) {
    super(container.$form());
    this.container = container;
  }

  /**
   * Sends an HTTP request to a server and receives generated text. The server has
   * an LSTM neural network that takes in "seed text" and outputs text based on
   * that seed text.
   */

  @SimpleFunction
  public String GetGeneratedText(final String seedText) {
    String generatedStr = "";
    StringBuffer responseBuf = new StringBuffer();
    try {
      String encodedSeed = java.net.URLEncoder.encode(seedText, "UTF-8").replaceAll("\\+", "%20");
      URL urlObj = new URL(baseURLString + "?inputText=" + encodedSeed);
      HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
      con.setRequestMethod("GET");
      con.setConnectTimeout(300000);
      con.setReadTimeout(300000);
      con.setRequestProperty("Content-Type", "application/json");
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;

      while ((inputLine = in.readLine()) != null) {
        responseBuf.append(inputLine);
        responseBuf.append("\n");
      }
      in.close();
      con.disconnect();
    } catch (IOException e) {
      if (DEBUG) {
        Log.d(LOG_TAG, e.toString());
      }
      responseBuf.append(e.toString());
    }
    String responseString = responseBuf.toString();
    try {
      JSONObject jsonObj = new JSONObject(responseString);
      responseString = jsonObj.getString("generated");
    } catch (JSONException e) {
      responseString = e.toString();
    }

    return responseString;
  }
}