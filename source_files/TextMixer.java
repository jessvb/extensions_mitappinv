
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
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.annotations.SimpleEvent;
import java.net.*;
import java.io.*;
import org.json.*;

import android.app.Activity;
import android.util.Log;

@DesignerComponent(version = TextMixer.VERSION, description = "Generates text using a seed sentence input and mixing text generation based on Dr. Seuss, Shakespeare, and Taylor Swift.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "appengine/src/com/google/appinventor/images/TODO.png")
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

  /**
   * Generate text.
   * 
   * @param seedSentence
   * @param drSeussPercentage
   * @param taylorSwiftPercentage
   * @param shakespearePercentage
   */
  @SimpleFunction
  public void StartSentenceGeneration(final String seedSentence, final float drSeussPercentage,
      final float taylorSwiftPercentage, final float shakespearePercentage) {
    // From Web.java:
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          // From MediaStore.java:
          String encodedSeed = java.net.URLEncoder.encode(seedSentence, "UTF-8").replaceAll("\\+", "%20"); // Unclear if
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

          final String responseString = response.toString();

          // send everything to GotGeneratedSentence and GotGeneratedSentenceAndTexts:
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              // chop off the warning (if there is one), so it's just the json:
              String finalSentence = responseString.substring(responseString.indexOf("{"));
              YailList todoDelme = new YailList();
              try {
                JSONObject jsonObj = new JSONObject(finalSentence.toString());
                // {"tokens": ["hello", "world", "in", "the", "clear", "to", "a", "little",
                // "<eos>"], "corpora": ["none", "none", "seuss", "seuss", "taylor",
                // "shakespeare", "seuss", "taylor", "shakespeare"]}
                finalSentence = "";
                JSONArray tokenArr = jsonObj.getJSONArray("tokens");
                // todoDelme = YailList.makeList(jsonArr.toArray()); TODO
                todoDelme = YailList.makeList(new String[] {"this one", "you two"});
                for (int i = 0; i < tokenArr.length(); i++) {
                  // don't add the last <eos>
                  if (i < tokenArr.length() - 1) {
                    finalSentence += tokenArr.getString(i);
                    // add a space between words (if not at end):
                    if (i < tokenArr.length() - 2) {
                      finalSentence += " ";
                    }
                  }
                }
              } catch (JSONException e) {
                finalSentence = e.toString();
              }
              if (responseString.contains("Warning")) {
                finalSentence += " | Warning: The percentages must add up to one.";
              }

              // Dispatch the events:
              GotGeneratedSentence(finalSentence);
              GotGeneratedSentenceAndTexts(todoDelme, todoDelme);
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
   * Indicates that a StartSentenceGeneration server request has succeeded.
   * Returns the generated sentence.
   *
   * @param sentence the value that was returned.
   */
  @SimpleEvent
  public void GotGeneratedSentence(String sentence) {
    EventDispatcher.dispatchEvent(this, "GotGeneratedSentence", sentence);
  }

  /**
   * Indicates that a StartSentenceGeneration server request has succeeded.
   * Returns the generated sentence and the text each word came from (e.g., Dr.
   * Seuss, Shakespeare, Taylor Swift corpora).
   *
   * @param wordList       the list of words that were returned.
   * @param textOriginList the list of texts where each word came from.
   */
  @SimpleEvent
  public void GotGeneratedSentenceAndTexts(YailList wordList, YailList textOriginList) {
    EventDispatcher.dispatchEvent(this, "GotGeneratedSentenceAndTexts", wordList, textOriginList);
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