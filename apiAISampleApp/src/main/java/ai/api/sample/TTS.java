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

package ai.api.sample;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class TTS implements TextToSpeech.OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

    private static TextToSpeech textToSpeech;

    public static void init(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                Log.d("@@", "start");
                                if (mListener != null ) {mListener.onStart(utteranceId);}
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                Log.d("@@", "done");
                                if (mListener != null ) {mListener.onDone(utteranceId);}
                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.d("@@", "error");
                                if (mListener != null ) {mListener.onError(utteranceId);}
                            }
                        });
                    }
                }
            });
            textToSpeech.setLanguage(new Locale("vi_VN"));
        }
    }


    public interface ISpeakListener {
        void onStart(String key);
        void onError(String key);
        void onDone(String key);
    }

    static ISpeakListener mListener;
    public static void setSpeak(ISpeakListener listener) {
        mListener = listener;
    }

    public static void speak(String text) {
        speak(text, "unknown");
    }

    public static void speak(String text, String key) {
        HashMap<String, String> myHashAlarm = new HashMap<>();
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, key);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
    }

//    private void speak(String text) {
//        if(text != null) {
//            HashMap<String, String> myHashAlarm = new HashMap<String, String>();
//            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
//            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SOME MESSAGE");
//            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, myHashAlarm);
//
//        }
//    }

    //    // Fired after TTS initialization
//    public void onInit(int status) {
//        if(status == TextToSpeech.SUCCESS) {
//            textToSpeech.setOnUtteranceCompletedListener(this);
//        }
//    }
    // It's callback
    @Override
    public void onUtteranceCompleted(String utteranceId) {
        Log.d("@@", "audio done");
    }

    @Override
    public void onInit(int status) {

    }
}
