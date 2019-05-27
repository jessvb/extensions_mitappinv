
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
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.annotations.SimpleEvent;
import java.net.*;
import java.io.*;
import org.json.*;

import android.app.Activity;
import android.util.Log;

@DesignerComponent(version = SentenceGenerator.VERSION, description = "Generates text using input seed text.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "appengine/src/com/google/appinventor/images/TODO.png")
@SimpleObject(external = true)
public class SentenceGenerator extends AndroidNonvisibleComponent implements Component {
  public static final int VERSION = 1;
  private ComponentContainer container;
  private final boolean DEBUG = true;
  private final String LOG_TAG = "SentenceGenerator";
  private final String baseURLString = "http://appinventor-alexa.csail.mit.edu:1234/";
  private final Activity activity; // for running on ui thread

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
    activity = container.$context();
  }

  @SimpleFunction
  public void StartTextGeneration(final String seedText, final int outputLength) {
    // From Web.java:
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          // From MediaStore.java:
          String url = baseURLString + "?inputText=" + seedText + "&model=drSeuss_20" + "&outputLength=" + outputLength;
          URL obj = new URL(url);
          HttpURLConnection con = (HttpURLConnection) obj.openConnection();

          // optional default is GET
          con.setRequestMethod("GET");

          // add request header
          String USER_AGENT = "AppInventor";
          con.setRequestProperty("User-Agent", USER_AGENT);
          con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

          con.setConnectTimeout(300000); // 5 min
          con.setReadTimeout(300000);

          // get and build sentence from response
          BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
          StringBuilder response = new StringBuilder();
          String inputLine;
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          in.close();

          String tempResponseString = response.toString();
          try {
            JSONObject jsonObj = new JSONObject(tempResponseString);
            tempResponseString = jsonObj.getString("generated");
          } catch (JSONException e) {
            tempResponseString = e.toString();
          }
          final String responseString = tempResponseString;

          // send sentence to GotGeneratedText:
          // Dispatch the event.
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotGeneratedText(responseString);
            }
          });
        } catch (Exception e) {
          Log.e(LOG_TAG, "ERROR_UNABLE_TO_GET", e);
          e.printStackTrace();
          WebServiceError(e.toString());
        }
      }
    });
  }

  /**
   * Indicates that a StartTextGeneration server request has succeeded.
   *
   * @param sentence the value that was returned.
   */
  @SimpleEvent
  public void GotGeneratedText(String sentence) {
    EventDispatcher.dispatchEvent(this, "GotGeneratedText", sentence);
  }

  /**
   * Indicates that the communication with the Web service signaled an error
   *
   * @param message the error message
   */
  @SimpleEvent
  public void WebServiceError(String message) {
    EventDispatcher.dispatchEvent(this, "WebServiceError", message);
  }
}