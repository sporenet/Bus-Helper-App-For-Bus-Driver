package com.sporenet123.bushelperserver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";

    PHPExecutor mPHPExecutor;

    Button mBusNumberInfoButton;
    Button mBusNumberInput0Button;
    Button mBusNumberInput1Button;
    Button mBusNumberInput2Button;
    Button mBusNumberInput3Button;
    Button mBusNumberInput4Button;
    Button mBusNumberInput5Button;
    Button mBusNumberInput6Button;
    Button mBusNumberInput7Button;
    Button mBusNumberInput8Button;
    Button mBusNumberInput9Button;
    Button mGPSInfoButton;

    EditText mGPSLatitudeText;
    EditText mGPSLongitudeText;

    TextView mDebugTextView;

    LocationManager mLocationManager;
    LocationListener mLocationListener;

    float mStationLatitude = 0;
    float mStationLongitude = 0;
    float mCurrentLatitude = 0;
    float mCurrentLongitude = 0;
    float mDist = 0;

    TextToSpeech mTextToSpeech;

    private static final int MIN_TIME_BW_UPDATES_DISTANCE = 1000 * 10;       // 10초마다 업데이트
    private static final int MIN_TIME_BW_UPDATES_GETOFF = 1000 * 5;       // 5초마다 업데이트
    private static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 2m마다 업데이트
    private static final String mPHPFileAddr = "http://147.46.215.187:8080/topis/delete_off_table.php";
    private static final String mPHPResultAddr = "http://147.46.215.187:8080/topis/delete_off_result.xml";
    private static final String mBluetoothMacAddr = "D0:FF:50:66:94:D6";

    String mBusNumber = "";
    String mBusNumberInfoText1 = "버스 번호 : ";
    String mBusNumberInfoText2 = " (완료하려면 클릭)";
    String mBusNumberInfoText3 = " (서비스 실행중)";
    String mBusNumberInfoText;

    boolean mIsBusNumberInputCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBusNumberInfoButton = (Button)findViewById(R.id.busNumberInfoButton);
        mBusNumberInput0Button = (Button)findViewById(R.id.busNumberInput0Button);
        mBusNumberInput1Button = (Button)findViewById(R.id.busNumberInput1Button);
        mBusNumberInput2Button = (Button)findViewById(R.id.busNumberInput2Button);
        mBusNumberInput3Button = (Button)findViewById(R.id.busNumberInput3Button);
        mBusNumberInput4Button = (Button)findViewById(R.id.busNumberInput4Button);
        mBusNumberInput5Button = (Button)findViewById(R.id.busNumberInput5Button);
        mBusNumberInput6Button = (Button)findViewById(R.id.busNumberInput6Button);
        mBusNumberInput7Button = (Button)findViewById(R.id.busNumberInput7Button);
        mBusNumberInput8Button = (Button)findViewById(R.id.busNumberInput8Button);
        mBusNumberInput9Button = (Button)findViewById(R.id.busNumberInput9Button);
        mGPSInfoButton = (Button)findViewById(R.id.gpsInfoButton);

        mGPSLatitudeText = (EditText)findViewById(R.id.gpsLatText);
        mGPSLongitudeText = (EditText)findViewById(R.id.gpsLonText);

        mDebugTextView = (TextView)findViewById(R.id.debugTextView);

        // 하차하려는 사람을 주기적으로 검사
        final Timer mTimer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                mPHPExecutor = new PHPExecutor();
                mPHPExecutor.execute(mPHPFileAddr);
                String result = "0";

                try {
                    URL url = new URL(mPHPResultAddr);
                    InputStream in = url.openStream();

                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser parser = factory.newPullParser();

                    parser.setInput(in, "euc-kr");

                    parser.next();
                    String tagName = parser.getName();
                    Log.d(TAG, tagName);

                    if (tagName.equals("result")) {
                        parser.next();
                        result = parser.getText();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Xml parsing failed.");
                }

                if (result.equals("1")) {
                    speakOutGetOff();
                }
            }
        };

        // 버스 번호 입력 부분
        mBusNumberInfoText = mBusNumberInfoText1 + mBusNumberInfoText2;
        mBusNumberInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText3;
                mBusNumberInfoButton.setText(mBusNumberInfoText);
                mTimer.schedule(task, 0, MIN_TIME_BW_UPDATES_GETOFF);
                mBusNumberInfoButton.setEnabled(false);
                mIsBusNumberInputCompleted = true;
            }
        });
        mBusNumberInput0Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "0";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "1";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "2";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput3Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "3";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput4Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "4";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput5Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "5";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput6Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "6";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput7Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "7";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput8Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "8";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });
        mBusNumberInput9Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIsBusNumberInputCompleted) {
                    mBusNumber += "9";
                    mBusNumberInfoText = mBusNumberInfoText1 + mBusNumber + mBusNumberInfoText2;
                    mBusNumberInfoButton.setText(mBusNumberInfoText);
                }
            }
        });

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BW_UPDATES_DISTANCE,
                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                mLocationListener
        );

        mGPSInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 무선 네트워크 사용 여부, GPS 연결 여부
                boolean isInternetGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (isInternetGPSEnabled && isGPSEnabled) {
                    if (mGPSLatitudeText.getText().toString().equals("") || mGPSLongitudeText.getText().toString().equals("")) {
                        Toast toast = Toast.makeText(getApplicationContext(), "위도와 경도를 올바르게 입력하세요", Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        mStationLatitude = Float.parseFloat(mGPSLatitudeText.getText().toString());
                        mStationLongitude = Float.parseFloat(mGPSLongitudeText.getText().toString());
                        Toast toast = Toast.makeText(getApplicationContext(), "GPS 좌표 전송 완료!", Toast.LENGTH_SHORT);
                        toast.show();
                        String debugText = "버스 위도 : " + Float.toString(mCurrentLatitude) + ", 버스 경도 : " + Float.toString(mCurrentLongitude) +
                                "\n정류장 위도 : " + Float.toString(mStationLatitude) + " , 정류장 경도 : " + Float.toString(mStationLongitude) +
                                "\n거리 : " + Float.toString(mDist);
                        mDebugTextView.setText(debugText);
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "GPS 기능을 실행시켜주세요", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        mTextToSpeech = new TextToSpeech(this, new TextToSpeechListener());
    }

    public class PHPExecutor extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... values) {
            String msg = "Failed to connect to server.";

            try {
                String url = values[0];

                URL text = new URL(url + "?bus_num=" + mBusNumber);

                HttpURLConnection conn = (HttpURLConnection)text.openConnection();

                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setUseCaches(false);

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        msg = br.readLine();
                        br.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return msg;
        }
    }

    public class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                mCurrentLatitude = (float)location.getLatitude();
                mCurrentLongitude = (float)location.getLongitude();
                mDist = distFrom(mCurrentLatitude, mCurrentLongitude, mStationLatitude, mStationLongitude);
                if (mDist < 50) {
                    speakOutWaiting();
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    public void speakOutGetOff() {
        String text = "하차하려는 사람이 있습니다";
        if (mTextToSpeech != null)
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void speakOutWaiting() {
        String text = "시각장애인이 버스정류장에 있습니다";
        if (mTextToSpeech != null)
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public class TextToSpeechListener implements TextToSpeech.OnInitListener{
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTextToSpeech.setLanguage(Locale.KOREA);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This language is not supported.");
                }
            } else {
                Log.e("TTS", "Initialization of TextToSpeech failed");
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        super.onDestroy();
    }
}