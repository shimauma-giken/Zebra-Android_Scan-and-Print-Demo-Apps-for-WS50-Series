// **********************************************************************************************
// *                                                                                            *
// *    This application is intended for demonstration purposes only. It is provided as-is      *
// *    without guarantee or warranty and may be modified to suit individual needs.             *
// *                                                                                            *
// **********************************************************************************************

package com.zebra.basicintent1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.comm.Connection;


public class MainActivity extends AppCompatActivity {

    String logvTag = "ZZZ";

    //
    // The section snippet below registers to receive the data broadcast from the
    // DataWedge intent output. In the example, a dynamic broadcast receiver is
    // registered in the onCreate() call of the target app. Notice that the filtered action
    // matches the "Intent action" specified in the DataWedge Intent Output configuration.
    //
    // For a production app, a more efficient way to the register and unregister the receiver
    // might be to use the onResume() and onPause() calls.

    // Note: If DataWedge had been configured to start an activity (instead of a broadcast),
    // the intent could be handled in the app's manifest by calling getIntent() in onCreate().
    // If configured as startService, then a service must be created to receive the intent.
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.activity_intent_filter_action));
        registerReceiver(myBroadcastReceiver, filter);

        Button btnPrt = (Button)findViewById(R.id.btnPrt);
        btnPrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logv("btn clicked");
                String theBtMacAddress = "AC:3F:A4:B8:B2:33";

                final TextView lblScanData = (TextView) findViewById(R.id.lblScanData);
                String barcode = lblScanData.getText().toString();
                String epc = "AAA00" + barcode + "00";
                sendZplOverBluetooth(theBtMacAddress, barcode, epc);
            }
        });

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }

    //
    // After registering the broadcast receiver, the next step (below) is to define it.
    // Here it's done in the MainActivity.java, but also can be handled by a separate class.
    // The logic of extracting the scanned data and displaying it on the screen
    // is executed in its own method (later in the code). Note the use of the
    // extra keys defined in the strings.xml file.
    //
    private BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();

            //  This is useful for debugging to verify the format of received intents from DataWedge
            //for (String key : b.keySet())
            //{
            //    Log.v(LOG_TAG, key);
            //}

            if (action.equals(getResources().getString(R.string.activity_intent_filter_action))) {
                //  Received a barcode scan
                try {
                    displayScanResult(intent, "via Broadcast");
                } catch (Exception e) {
                    //  Catch if the UI does not exist when we receive the broadcast
                }
            }
        }
    };

    //
    // The section below assumes that a UI exists in which to place the data. A production
    // application would be driving much of the behavior following a scan.
    //
    private void displayScanResult(Intent initiatingIntent, String howDataReceived)
    {
        String decodedSource = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_source));
        String decodedData = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_data));
        String decodedLabelType = initiatingIntent.getStringExtra(getResources().getString(R.string.datawedge_intent_key_label_type));

        final TextView lblScanSource = (TextView) findViewById(R.id.lblScanSource);
        final TextView lblScanData = (TextView) findViewById(R.id.lblScanData);
        final TextView lblScanLabelType = (TextView) findViewById(R.id.lblScanDecoder);

        lblScanSource.setText(decodedSource + " " + howDataReceived);
        lblScanData.setText(decodedData);
        lblScanLabelType.setText(decodedLabelType);
    }


    public void logv(String msg) { Log.v(logvTag, msg);}

    private void sendZplOverBluetooth(final String theBtMacAddress, String barcode, String epc) {

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Instantiate insecure connection for given Bluetooth&reg; MAC Address.
                    Connection thePrinterConn = new BluetoothConnectionInsecure(theBtMacAddress);

                    // Initialize
                    Looper.prepare();

                    // Open the connection - physical connection is established here.
                    thePrinterConn.open();

                    // This example prints "This is a ZPL test." near the top of the label.
                    //String zplData = "^XA^FO50,50^A0N,30,30^FDRFID Label will be printed/encoded.^FS^XZ";
                    /*
                    // ZPL Example
                    ^XA
                    ^DFE:SAMPLE01.ZPL^FS
                    ~TA000
                    ~JSN
                    ^LT0
                    ^MNM,0
                    ^MTT
                    ^PON
                    ^PMN
                    ^LH0,0
                    ^JMA
                    ^PR8,8
                    ~SD15
                    ^JUS
                    ^LRN
                    ^CI27
                    ^PA0,1,1,0
                    ^RS8,,,3
                    ^MMT
                    ^PW609
                    ^LL609
                    ^LS0
                    ^FT58,88^A0N,25,25^FH\^CI28^FDScan Data^FS^CI27
                    ^FT58,231^A0N,25,25^FH\^CI28^FDRFID Encode Data^FS^CI27
                    ^BY2,3,74^FT58,178^BCN,,Y,N,,A
                    ^FN1"bar"^FS
                    ^BY2,3,75^FT58,322^BCN,,Y,N,,A
                    ^FN2"epc"^FS
                    ^RFW,H,1,2,1^FD3000^FS
                    ^RFW,H,2,12,1^FN2"epc"^FS
                    ^XZ


                    ^XA
                    ^XFE:SAMPLE01.ZPL^FS
                    ^CI27^FN1^FH\^FD123456789012^FS
                    ^CI27^FN2^FH\^FD123456789012345678901234^FS
                    ^PQ1,0,1
                    ^XZ
                     */

                    String zplDataPart1 = """
                            ^XA
                            ^XFE:SAMPLE01.ZPL^FS
                            ^CI27^FN1^FH\\^FD
                            """;

                            //123456789012

                    String zplDataPart2 = """
                            ^FS
                            ^CI27^FN2^FH\\^FD
                            """;

                            //123456789012345678901234

                    String zplDataPart99 = """
                            ^FS
                            ^PQ1,0,1
                            ^XZ
                            """;


                    String zplData = zplDataPart1 + barcode + zplDataPart2 + epc + zplDataPart99;

                    // Send the data to printer as a byte array.
                    thePrinterConn.write(zplData.getBytes());

                    // Make sure the data got to the printer before closing the connection
                    Thread.sleep(500);

                    // Close the insecure connection to release resources.
                    thePrinterConn.close();

                    Looper.myLooper().quit();
                } catch (Exception e) {
                    // Handle communications error here.
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
