package jp.physicalyze.dgmeter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;

public class MainActivity extends Activity {
    // Bluetooth SPPを使用するときの決められたID
    final UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter bta;
    BluetoothSocket btSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView)findViewById(R.id.textView);
        StringBuilder sb = new StringBuilder();

        bta = BluetoothAdapter.getDefaultAdapter();

        if (bta.isEnabled()) {
            sb.append("ペアリングOK\n");
        } else {
            sb.append("ペアリングNG\n");
        }

        // ペアリングしているBluetoothデバイスを検索
        Set<BluetoothDevice> bondedDevices = bta.getBondedDevices();
        BluetoothDevice btDevice = null;
        for (BluetoothDevice dev : bondedDevices) {
            sb.append("ペアリング端末: " );
            sb.append(dev.getName());
            sb.append(" (" );
            sb.append(dev.getAddress() );
            sb.append(")\n");
            // RNBTで始まるBluetooth端末を通信先にする
            if (dev.getName().startsWith("RNBT")) {
                btDevice = dev;
            }
        }

        if (btDevice != null) {
            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(sppUuid);
                btSocket.connect();


            } catch (IOException ex) {
                sb.append("接続できませんでした: " );
                sb.append(ex.toString());
            }
        } else {
            sb.append("ペアリングしているRN42がみつかりませんでした");
        }

        tv.setText(sb.toString());
    }

    public void onClickSend(View v) {
        String command;
        TextView cmd = (TextView)findViewById(R.id.command);
        TextView dgNo = (TextView)findViewById(R.id.dgNo);
        TextView dot = (TextView)findViewById(R.id.dot);

        command = cmd.getText().toString() + dgNo.getText().toString() + dot.getText().toString();

        sendCommand(command);
        Toast.makeText(this, "Ｄメール送信完了", Toast.LENGTH_SHORT).show();

    }


    public void onClickPre(View v) {
        String command = "P";
        sendCommand(command);
    }

    public void onClickNext(View v) {
        String command = "N";
        sendCommand(command);
    }

    public void onClickTimer(View v) {
        String command = "T";
        sendCommand(command);
    }

    public void onClickDGM(View v) {
        String command = "D";
        sendCommand(command);
    }

    // コマンド送出
    private void sendCommand(String command) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(btSocket.getOutputStream(), "ASCII"));
            writer.write(command);
            writer.flush();
        } catch (IOException ex) {
            Toast.makeText(this, "Dメール送信に失敗しました: " + ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // NFCを読み込んで変更
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        TextView text = (TextView)findViewById(R.id.textView2);

        Parcelable[] raws = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage[] msgs = new NdefMessage[raws.length];
        String regex = "^[0-9]$";
        Pattern p = Pattern.compile(regex);
        String dgm = "C";
        for (int i = 0; i < raws.length; i++) {
            msgs[i] = (NdefMessage)raws[i];
            for (NdefRecord record : msgs[i].getRecords()) {
                byte[] payload = record.getPayload();
                if (payload == null) {
                    break;
                }
                for(byte data : payload) {
                    byte[] dataArr = {data};
                    String dataStr = new String(dataArr);
                    // 受け取るのは数値かスペースだけ
                    Matcher m = p.matcher(dataStr);
                    if (dataStr.equals(" ") || m.find()) {
                        dgm += dataStr;
                    }
                }
            }
        }
        text.setText("NFC読み込み内容: " + dgm);

        TextView cmd = (TextView)findViewById(R.id.command);
        TextView dgNo = (TextView)findViewById(R.id.dgNo);
        TextView dot = (TextView)findViewById(R.id.dot);

        cmd.setText("C");
        dgNo.setText(dgm.substring(1,9));
        dot.setText(dgm.substring(9,17));

        sendCommand(dgm);
        Toast.makeText(this, "Ｄメール送信完了", Toast.LENGTH_SHORT).show();
    }

}
