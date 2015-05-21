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
    PHPExecutor mPHPExecutor;
    Button mSetCurrentGPSButton;
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    TextView mBusNumberTextView;
    float mStationLatitude = 0;
    float mStationLongitude = 0;
    float mCurrentLatitude = 0;
    float mCurrentLongitude = 0;
    TextToSpeech mTextToSpeech;
    private static final int MIN_TIME_BW_UPDATES_DISTANCE = 1000 * 10;       // 10초마다 업데이트
    private static final int MIN_TIME_BW_UPDATES_GETOFF = 1000 * 5;       // 5초마다 업데이트
    private static final int MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 2m마다 업데이트
    private static final String mPHPFileAddr = "http://147.46.215.187:8080/topis/delete_off_table.php";
    private static final String mPHPResultAddr = "http://147.46.215.187:8080/topis/delete_off_result.xml";
    private static final String mBluetoothMacAddr = "D0:FF:50:66:94:D6";
    private static final String TAG = "MainActivity";
    String mBusNumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSetCurrentGPSButton = (Button)findViewById(R.id.setCurrentGPSButton);
        mBusNumberTextView = (TextView)findViewById(R.id.currentBusNumText);

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

        // 버스 번호 입력 다이얼로그
        AlertDialog.Builder mBusNumberDialog = new AlertDialog.Builder(this);
        mBusNumberDialog.setTitle(R.string.input_bus_number);

        final EditText mBusNumberText = new EditText(this);
        mBusNumberDialog.setView(mBusNumberText);

        mBusNumberDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBusNumber = mBusNumberText.getText().toString();
                mTimer.schedule(task, 0, MIN_TIME_BW_UPDATES_GETOFF);
                mBusNumberTextView.setText(mBusNumber);
            }
        });

        mBusNumberDialog.show();

        mSetCurrentGPSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStationLatitude = mCurrentLatitude;
                mStationLongitude = mCurrentLongitude;
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

        mTextToSpeech = new TextToSpeech(this, new TextToSpeechListener());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                float dist = distFrom(mCurrentLatitude, mCurrentLongitude, mStationLatitude, mStationLongitude);
                if (dist < 50) {
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