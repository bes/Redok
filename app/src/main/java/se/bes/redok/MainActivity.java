package se.bes.redok;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import se.bes.redok.device.DeviceApi;
import se.bes.redok.device.NetRemote;
import se.bes.redok.fsapi.FsApi;
import se.bes.redok.fsapi.FsApiResponse;

public class MainActivity extends AppCompatActivity {

    public static final String FS_PIN = "1234";

    private FsApi mFsApi;

    Button radioButton;
    Button spotifyButton;
    ProgressBar progressBar;
    Button volumeUp;
    Button volumeDown;
    TextView volumeValueText;

    ConnectTask connectTask;

    SetVolumeTask setVolumeTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectTask = new ConnectTask(new WeakReference<>(this));
        connectTask.execute();

        setContentView(R.layout.activity_main);

        radioButton = (Button) findViewById(R.id.radio_button);
        radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ModeTask(mFsApi, FsApi.Mode.RADIO).execute();
            }
        });

        spotifyButton = (Button) findViewById(R.id.spotify_button);
        spotifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ModeTask(mFsApi, FsApi.Mode.SPOTIFY).execute();
            }
        });

        volumeValueText = (TextView) findViewById(R.id.volume_value_text);

        volumeUp = (Button) findViewById(R.id.volume_up);
        volumeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentValue = Integer.parseInt((String) volumeValueText.getText());
                int nextValue = currentValue + 1;
                volumeValueText.setText(String.valueOf(nextValue));
                if (setVolumeTask != null) {
                    setVolumeTask.cancel(true);
                }
                setVolumeTask = new SetVolumeTask(mFsApi, new WeakReference<>(MainActivity.this), nextValue);
                setVolumeTask.execute();
            }
        });

        volumeDown = (Button) findViewById(R.id.volume_down);
        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentValue = Integer.parseInt((String) volumeValueText.getText());
                int nextValue = currentValue - 1;
                volumeValueText.setText(String.valueOf(nextValue));
                if (setVolumeTask != null) {
                    setVolumeTask.cancel(true);
                }
                setVolumeTask = new SetVolumeTask(mFsApi, new WeakReference<>(MainActivity.this), nextValue);
                setVolumeTask.execute();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.connecting_progressbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectTask.cancel(true);
    }

    public void setFsApi(FsApi fsApi) {
        mFsApi = fsApi;
        if (mFsApi != null) {
            progressBar.setVisibility(View.GONE);
            radioButton.setEnabled(true);
            spotifyButton.setEnabled(true);
            volumeUp.setEnabled(true);
            volumeDown.setEnabled(true);
            updateVolumeLabel();
        }
    }

    public void setVolumeLabel(FsApiResponse fsApiResponse) {
        if (fsApiResponse != null) {
            volumeValueText.setText(String.valueOf(fsApiResponse.getU8()));
        }
    }

    public void updateVolumeLabel() {
        new GetVolumeTask(mFsApi, new WeakReference<>(this)).execute();
    }

    @RequiredArgsConstructor
    static class ConnectTask extends AsyncTask<Void, Void, FsApi> {

        private final WeakReference<MainActivity> mMainActivityWeak;

        @Override
        protected FsApi doInBackground(Void... params) {
            try {
                InetAddress inetAddress = InetAddress.getByName("239.255.255.250");
                String message = String.format("M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: %s:%s\r\n" +
                        "MAN: \"ssdp:discover\"\r\n" +
                        "ST: %s\r\n" +
                        "MX: 3\r\n" +
                        "\r\n" +
                        "\r\n", "239.255.255.250", "1900", "urn:schemas-frontier-silicon-com:argon_001:fsapi:1");

                byte[] buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, 1900);
                DatagramSocket socket = new DatagramSocket();
                socket.send(packet);

                byte[] buffer = new byte[1024];
                DatagramPacket recvpacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(recvpacket);
                String data = new String(recvpacket.getData(), 0, recvpacket.getLength());

                String[] rows = data.split("\r\n");
                Map<String, String> map = new HashMap<>();
                for (int i = 0; i < rows.length; i++) {
                    String r = rows[i];
                    int idx = r.indexOf(':');
                    if (idx != -1) {
                        map.put(r.substring(0, idx).trim().toLowerCase(), r.substring(idx + 1).trim());
                    }
                }

                String location = map.get("location");
                String usn = map.get("usn");
                String st = map.get("st");
                String cache = map.get("cache-control").split("=")[1];

                System.out.println(location);

                DeviceApi api = discover(location + "/");

                NetRemote netRemote = api.getDevice().execute().body();

                return fsApi(netRemote.getWebfsapi() + "/");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(FsApi fsApi) {
            MainActivity mainActivity = mMainActivityWeak.get();
            if (mainActivity != null) {
                mainActivity.setFsApi(fsApi);
            }
        }

        private DeviceApi discover(String endpoint) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(endpoint)
                    .client(client)
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .build();

            return retrofit.create(DeviceApi.class);
        }

        private FsApi fsApi(String endpoint) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(endpoint)
                    .client(client)
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .build();

            return retrofit.create(FsApi.class);
        }
    }

    @RequiredArgsConstructor
    static class ModeTask extends AsyncTask<Void, Void, Void> {

        private final FsApi mFsApi;
        private final FsApi.Mode mMode;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                FsApiResponse fsApiResponse = mFsApi.setSysMode(FS_PIN, mMode.mode).execute().body();
                System.out.println(fsApiResponse.getStatus());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @RequiredArgsConstructor
    static class GetVolumeTask extends AsyncTask<Void, Void, FsApiResponse> {

        private final FsApi mFsApi;

        private final WeakReference<MainActivity> mMainActivityWeak;

        @Override
        protected FsApiResponse doInBackground(Void... params) {
            try {
                return mFsApi.getSysAudioVolume(FS_PIN).execute().body();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(FsApiResponse fsApiResponse) {
            MainActivity mainActivity = mMainActivityWeak.get();
            if (mainActivity != null) {
                mainActivity.setVolumeLabel(fsApiResponse);
            }
        }
    }

    @RequiredArgsConstructor
    static class SetVolumeTask extends AsyncTask<Void, Void, FsApiResponse> {

        private final FsApi mFsApi;

        private final WeakReference<MainActivity> mMainActivityWeak;

        private final int volume;

        @Override
        protected FsApiResponse doInBackground(Void... params) {
            try {
                return mFsApi.setSysAudioVolume(FS_PIN, volume).execute().body();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(FsApiResponse fsApiResponse) {
            MainActivity mainActivity = mMainActivityWeak.get();
            if (mainActivity != null) {
                mainActivity.updateVolumeLabel();
            }
        }
    }
}
