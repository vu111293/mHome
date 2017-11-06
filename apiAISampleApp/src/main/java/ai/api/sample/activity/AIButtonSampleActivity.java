/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.api.sample.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.sample.R;
import ai.api.sample.TTS;
import ai.api.sample.base.BaseActivity;
import ai.api.sample.config.Config;
import ai.api.ui.AIButton;

public class AIButtonSampleActivity extends BaseActivity implements AIButton.AIButtonListener,
        TTS.ISpeakListener, View.OnClickListener {

    public static final String TAG = AIButtonSampleActivity.class.getName();

    private AIButton aiButton;
    private TextView resultTextView;
    private Gson gson = GsonFactory.getGson();
    private AIConfiguration config;

    public static final String SPEAK_KEY = "sample";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aibutton_sample);

        findViewById(R.id.btn_wifi).setOnClickListener(this);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.micButton);
        config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);

        TTS.init(this);
        TTS.setSpeak(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        aiButton.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // use this method to reinit connection to recognition service
        aiButton.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_aibutton_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(AISettingsActivity.class);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStartRecording() {
        Log.d(TAG, "onStart");
        isRecording = true;
    }

    @Override
    public void onResult(final AIResponse response) {
        runOnUiThread(new Runnable() {
            @TargetApi(Build.VERSION_CODES.GINGERBREAD)
            @Override
            public void run() {
                Log.d(TAG, "onResult");

                resultTextView.setText(gson.toJson(response));

                Log.i(TAG, "Received success response");

                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

                Log.i(TAG, "Action: " + result.getAction());
                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);

                if (speech != null && !speech.isEmpty()) {
                    TTS.speak(speech, SPEAK_KEY);
                } else {
                    TTS.speak("Xin nhắc lại", SPEAK_KEY);
                }


//                final Metadata metadata = result.getMetadata();
//                if (metadata != null) {
//                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
//                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
//                }
//
//                final HashMap<String, JsonElement> params = result.getParameters();
//                if (params != null && !params.isEmpty()) {
//                    Log.i(TAG, "Parameters: ");
//                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
//                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
//                    }
//                }
            }

        });

        // aiButton.startListening();
//        startRecording();
    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });
//        aiButton.pause();
//        aiButton.startListening();

        // startRecording();
        aiButton.initialize(config);
        TTS.speak("Xin nhắc lại", SPEAK_KEY);

        // aiButton.startListening();
//        startRecording();
    }

    @Override
    public void onCancelled() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "onCancelled");
//                resultTextView.setText("");
//            }
//        });
////        aiButton.pause();
////        aiButton.startListening();
////        startRecording();
//
//        aiButton.initialize(config);
//        TTS.speak("Xin nhắc lại", SPEAK_KEY);
//        aiButton.startListening();
    }

    private void startActivity(Class<?> cls) {
        final Intent intent = new Intent(this, cls);
        startActivity(intent);
    }


    Handler recordHandler;
    Runnable recordRunnable;
    boolean isRecording = false;

    private void startRecording() {
        if (recordHandler != null) {
            recordHandler.removeCallbacks(recordRunnable);
            recordHandler = null;
        }


        isRecording = false;
        aiButton.pause();
        recordHandler = new Handler();
        recordRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRecording) {
                    aiButton.pause();
                    aiButton.startListening();
                    recordHandler.postDelayed(recordRunnable, 1000);
                }
            }
        };
        recordHandler.postDelayed(recordRunnable, 1000);


    }

    @Override
    public void onStart(String key) {

    }

    @Override
    public void onError(String key) {
        if (key.equals("sample")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });
        }
    }

    @Override
    public void onDone(String key) {
        if (key.equals("sample")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_wifi:
                break;

            default:
                break;
        }
    }
}
