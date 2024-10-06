package com.example.mdp_final;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private TextView textView, bluetoothLogView, textView2, textView3;
    private Button button1, button2;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private ListView deviceListView;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private String previousData = "";
    private boolean isListening = false;
    private boolean isListeningForData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        bluetoothLogView = findViewById(R.id.bluetooth_log);
        button1 = findViewById(R.id.button);
        button2 = findViewById(R.id.search_devices);
        deviceListView = findViewById(R.id.device_list);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = bluetoothDevices.get(position);
            connectToDevice(selectedDevice);
        });

        initializeSpeechRecognizer();
        initializeTextToSpeech();
        initializePermissionLauncher();

        button1.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            }
        });

        button2.setOnClickListener(v -> {
            if (!isListeningForData) {
                checkBluetoothPermissions();
                isListeningForData = true;
            }
        });
    }

    private void disableButton(Button button) {
        runOnUiThread(() -> button.setEnabled(false));
    }

    private void enableButton(Button button) {
        runOnUiThread(() -> button.setEnabled(true));
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "음성을 입력하세요...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() { }

            @Override
            public void onRmsChanged(float rmsdB) { }

            @Override
            public void onBufferReceived(byte[] buffer) { }

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                enableButton(button1);
            }

            @Override
            public void onError(int error) {
                isListening = false;
                enableButton(button1);
                String errorMessage = getErrorMessage(error);
                textView3.setText("에러 발생: " + errorMessage);
                startListening();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    textView.setText("음성 인식 결과: " + recognizedText);
                    handleRecognizedText(recognizedText);
                } else {
                    Log.d("SpeechRecognition", "No results or empty results received.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) { }

            @Override
            public void onEvent(int eventType, Bundle params) { }
        });
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "오디오 입력 중 오류 발생";
            case SpeechRecognizer.ERROR_CLIENT:
                return "클라이언트 오류 발생";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "권한 부족";
            case SpeechRecognizer.ERROR_NETWORK:
                return "네트워크 오류 발생";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "네트워크 타임아웃";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "매치되는 결과 없음";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "인식기 바쁨";
            case SpeechRecognizer.ERROR_SERVER:
                return "서버 오류 발생";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "입력 시간 초과";
            default:
                return "알 수 없는 오류 발생";
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(MainActivity.this, "이 언어는 지원되지 않습니다.", Toast.LENGTH_SHORT).show();
                }
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) { }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> startListening());
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("TTS", "TTS 오류 발생: " + utteranceId);
                        runOnUiThread(() -> startListening());
                    }
                });
                speak("수동모드, 자동모드, 쇼핑몰 중 선택하세요.");
            } else {
                textView3.setText("TTS 초기화에 실패");
            }
        });
    }

    private void initializePermissionLauncher() {
        requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            Boolean locationPermission = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            if (locationPermission) {
                searchBluetoothDevices();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startListening() {
        if (speechRecognizer != null && !isListening) {
            if (textToSpeech != null && textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀하세요...");
            speechRecognizer.startListening(intent);
            isListening = true;
        }
    }

    private void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    private void handleRecognizedText(String text) {
        if (text.equals("종료")) {
            speak("종료");
            finish();
        } else if (text.equals("자동 모드")) {
            sendData("자동모드");
        } else if (text.equals("수동 모드")) {
            sendData("수동모드");
        } else if (text.equals("쇼핑몰")) {
            sendData("쇼핑몰");
        } else {
            sendData(text);
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                searchBluetoothDevices();
            } else {
                requestPermissionsLauncher.launch(new String[]{Manifest.permission.BLUETOOTH_SCAN});
            }
        } else {
            searchBluetoothDevices();
        }
    }

    private void searchBluetoothDevices() {
        deviceListAdapter.clear();
        bluetoothDevices.clear();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                bluetoothDevices.add(device);
            }
            deviceListAdapter.notifyDataSetChanged();
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothReceiver, filter);
        bluetoothAdapter.startDiscovery();

        new Handler().postDelayed(() -> {
            enableButton(button2);
            isListeningForData = false;
        }, 10000);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                    bluetoothDevices.add(device);
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(this, "블루투스 디바이스가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            bluetoothLogView.setText("블루투스 연결 성공");
            speak("블루투스 연결 성공");
            listenForData();
        } catch (IOException e) {
            bluetoothLogView.setText("블루투스 연결 실패");
            Toast.makeText(this, "블루투스 연결 실패", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void listenForData() {
        Handler handler = new Handler();
        final byte[] buffer = new byte[1024];
        final boolean[] stopThread = {false};

        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopThread[0]) {
                try {
                    int bytesAvailable = bluetoothSocket.getInputStream().available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        bluetoothSocket.getInputStream().read(packetBytes);
                        String data = new String(packetBytes);
                        handler.post(() -> {
                            if (!data.equals(previousData)) {
                                textView2.append("Received Data: " + data + "\n");
                                previousData = data;
                                if (speechRecognizer != null && isListening) {
                                    speechRecognizer.stopListening();
                                    isListening = false;
                                }
                                speak("Received Data: " + data);
                            }
                        });
                    }
                } catch (IOException e) {
                    stopThread[0] = true;
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void sendData(String data) {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                // UTF-8로 인코딩하여 바이트 배열로 변환
                bluetoothSocket.getOutputStream().write(data.getBytes("UTF-8"));
                Toast.makeText(this, "Sent Data: " + data, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "블루투스 연결이 되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "데이터 전송 실패", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    private void speak(String text) {
        if (textToSpeech != null) {
            if (speechRecognizer != null && isListening) {
                speechRecognizer.stopListening();
                isListening = false;
            }

            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId");
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(bluetoothReceiver);
    }
}
