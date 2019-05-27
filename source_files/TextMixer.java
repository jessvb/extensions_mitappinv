
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

@DesignerComponent(version = TextMixer.VERSION, description = "Generates text using input seed text.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "appengine/src/com/google/appinventor/images/TODO.png")
@SimpleObject(external = true)
public class TextMixer extends AndroidNonvisibleComponent implements Component {
  public static final int VERSION = 1;
  private ComponentContainer container;
  private final boolean DEBUG = true;
  private final String LOG_TAG = "TextMixer";
  private final String baseURLString = "http://appinventor-alexa.csail.mit.edu:3000/";
  private final Activity activity; // for running on ui thread

  // example variable for extension
  private String exampleVar;

  // defaults:
  public static final String DEFAULT_EXAMPLE_VAR_VALUE = "default";

  /**
   * Constructor creates a new extension object with default values.
   */
  public TextMixer(ComponentContainer container) {
    super(container.$form());
    this.container = container;
    activity = container.$context();
  }

  @SimpleFunction
  public void StartTextGeneration(final String seedText, final float drSeussPercentage,
      final float taylorSwiftPercentage, final float shakespearePercentage) {
    // From Web.java:
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          // From MediaStore.java:
          String encodedSeed = java.net.URLEncoder.encode(seedText, "UTF-8").replaceAll("\\+", "%20"); // Unclear if
                                                                                                       // this is
                                                                                                       // necessary
          // e.g.,
          // http://appinventor-alexa.csail.mit.edu:3000/?sent=hello%20world&swift=.1&shakes=.1&seuss=.8
          String url = baseURLString + "?sent=" + encodedSeed + "&seuss=" + drSeussPercentage + "&swift="
              + taylorSwiftPercentage + "&shakes=" + shakespearePercentage;
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

          String tempResponseString = response.toString().substring(response.toString().indexOf("{"));
          try {
            JSONObject jsonObj = new JSONObject(tempResponseString.toString());
            tempResponseString = "";
            // {"tokens": ["hello", "world", "in", "the", "clear", "to", "a", "little",
            // "<eos>"], "corpora": ["none", "none", "seuss", "seuss", "taylor",
            // "shakespeare", "seuss", "taylor", "shakespeare"]}
            JSONArray jsonArr = jsonObj.getJSONArray("tokens");
            for (int i = 0; i < jsonArr.length(); i++) {
              // don't add the last <eos>
              if (i < jsonArr.length() - 1) {
                tempResponseString += jsonArr.getString(i);
                // add a space between words (if not at end):
                if (i < jsonArr.length() - 2) {
                  tempResponseString += " ";
                }
              }
            }
          } catch (JSONException e) {
            tempResponseString = e.toString();
          }
          if (response.toString().contains("Warning")) {
            tempResponseString += " | Warning: The percentages must add up to one.";
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