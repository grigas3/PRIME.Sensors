package com.mbientlab.tutorial.sensorfusion;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.moticon.insole3_service.Insole3Service;
import de.moticon.insole3_service.proto.Common;
import de.moticon.insole3_service.proto.Service;

//import androidx.localbroadcastmanager.content.LocalBroadcastManager;

//import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class HoloMoticonAgent  extends AppCompatActivity {

    private volatile boolean FOUNDRIGHT = false;
    private volatile boolean FOUNDLEFT = false;
    private volatile boolean CONNECTED = false;
    private volatile boolean CONNECTEDRIGHT = false;
    private volatile boolean CONNECTEDLEFT = false;
    private volatile boolean CONESTABLISHED = false;

    private volatile boolean CON_LEFT_ESTABLISHED = false;
    private volatile boolean CON_RIGHT_ESTABLISHED = false;

    private volatile boolean READYTOSTARTSERVICE = false;
    private volatile int insoleserial = 0;
    private volatile int insoleserialLEFT = 0;
    private volatile int insoleserialRIGHT = 0;

    private volatile boolean STARTINCOLESCONNECTION = false;

    private volatile int RIGHTSERVICECOUNTER = 0;
    private volatile int LEFTSERVICECOUNTER = 0;

    private Service.ConnectInsoles myconnectInsoles;
    private Service.InsoleDevice LEFTinsole;
    private Service.InsoleDevice RIGHTinsole;


    private volatile Service.MoticonMessage msg01;


    String message = "";
    private static String ip = "192.168.0.25";

    protected MulticastSocket socket = null;
    protected byte[] buf = new byte[256];

    private Context mainContext;




    public HoloMoticonAgent(Context _mainContext) {
        mainContext = _mainContext;

        bindService();

        LocalBroadcastManager.getInstance(mainContext).registerReceiver(createServiceMsgReceiver(), new IntentFilter(Insole3Service.BROADCAST_SERVICE_MSG));

        UDPRECEIVER ur = new UDPRECEIVER();
        ur.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        MSGSENDER m = new MSGSENDER();
        m.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }


    private BroadcastReceiver createServiceMsgReceiver() {

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                byte[] protoMsg = intent.getByteArrayExtra(Insole3Service.EXTRA_PROTO_MSG);
                Service.MoticonMessage moticonMessage;

                moticonMessage = Service.MoticonMessage.parseFrom(protoMsg);
                Log.d("test2021", moticonMessage.toString());

                Log.d("activity", moticonMessage.getMsgCase().name());

                if ((moticonMessage.getMsgCase().name() == "INSOLE_ADVERTISEMENT") && (CONNECTEDRIGHT == false || CONNECTEDLEFT == false)) {
                    Log.d("[Moticon message]", "RECEIVED AN ADV MESSAGE");

                    msg01 = moticonMessage;

                    if (moticonMessage.getInsoleAdvertisement().getInsole().getSide().name() == "LEFT" && FOUNDLEFT == false) {
                        LEFTinsole = moticonMessage.getInsoleAdvertisement().getInsole();
                        FOUNDLEFT = true;

                        //EditText myEditText2 = (EditText) findViewById(R.id.editText2);
                        //myEditText2.setText("Left OK");
                    }

                    if (moticonMessage.getInsoleAdvertisement().getInsole().getSide().name() == "RIGHT" && FOUNDRIGHT == false) {
                        RIGHTinsole = moticonMessage.getInsoleAdvertisement().getInsole();
                        FOUNDRIGHT = true;

                        //EditText myEditText = (EditText) findViewById(R.id.editText);
                        //myEditText.setText("Right OK");

                    }

                    if (FOUNDLEFT && FOUNDRIGHT) {
                        myconnectInsoles = Service.ConnectInsoles.newBuilder().addInsoles(LEFTinsole).addInsoles(RIGHTinsole).build();
                        Service.MoticonMessage.Builder moticonMessage2 = Service.MoticonMessage.newBuilder();
                        moticonMessage2.setConnectInsoles(myconnectInsoles);
                        Log.d("[Device IDs] ", LEFTinsole.getDeviceAddress() + ' ' + RIGHTinsole.getDeviceAddress());
                        Log.d("[# IDs] ", String.valueOf(myconnectInsoles.getInsolesCount()));

                        sendProtoToService(moticonMessage2.build().toByteArray());
                        CONNECTED = true;

                        //STOP SCANNING
                        Service.MoticonMessage.Builder moticonMessage4 = Service.MoticonMessage.newBuilder();
                        Service.StopInsoleScan sinscan = Service.StopInsoleScan.newBuilder().build();
                        moticonMessage4.setStopInsoleScan(sinscan);
                        sendProtoToService(moticonMessage4.build().toByteArray());
                        Log.d("M o t i c o n] ", moticonMessage4.getMsgCase().name());
                    }

                }


                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().name() == "DISCONNECTED") {
                    //EditText myEditText2 = (EditText) findViewById(R.id.editText2);
                    //myEditText2.setText("LEFT DISCONNECTED");
                    //EditText myEditText = (EditText) findViewById(R.id.editText);
                    //myEditText.setText("Right DISCONNECTED");
                }
                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().toString() == "READY" && moticonMessage.getInsoleConnectionStatus().getSide().name() == "LEFT") {
                    CON_LEFT_ESTABLISHED = true;
                }

                if ((moticonMessage.getMsgCase().name() == "INSOLE_CONNECTION_STATUS") && moticonMessage.getInsoleConnectionStatus().getStatus().toString() == "READY" && moticonMessage.getInsoleConnectionStatus().getSide().name() == "RIGHT") {
                    CON_RIGHT_ESTABLISHED = true;
                }

                if ((moticonMessage.getMsgCase().name() == "START_SERVICE_CONF")) {


                    RIGHTSERVICECOUNTER = moticonMessage.getStartServiceConf().getRightStartServiceConf().getServiceCounter();
                    Log.e("[SERVICE COUNTER] ", String.valueOf(RIGHTSERVICECOUNTER));

                    LEFTSERVICECOUNTER = moticonMessage.getStartServiceConf().getLeftStartServiceConf().getServiceCounter();
                    Log.e("[SERVICE COUNTER] ", String.valueOf(LEFTSERVICECOUNTER));
                }


                if ((moticonMessage.getMsgCase().name() == "STATUS_INFO") && (CON_LEFT_ESTABLISHED) && (CON_RIGHT_ESTABLISHED)) {
                    List<Service.InsoleStatusInfo> mysoles = moticonMessage.getStatusInfo().getInsoleStatusInfoList();

                    for (Service.InsoleStatusInfo insi : mysoles) {
                        if (insi.getInsoleInfo().getSide().name() == "LEFT")
                            insoleserialLEFT = insi.getInsoleInfo().getInsoleSettings().getSerialNumber();
                        else
                            insoleserialRIGHT = insi.getInsoleInfo().getInsoleSettings().getSerialNumber();
                    }

                    if (insoleserialLEFT > 0 && insoleserialRIGHT > 0)
                        READYTOSTARTSERVICE = true;
                }

            }
        };
        return br;
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {

        boolean mBound = false;
        private Insole3Service.Insole3Binder mInsole3Service;

        @Override
        public void onServiceConnected(ComponentName i, IBinder service) {
            Log.e("MOTICON", "on Service Connected");
            mInsole3Service = (Insole3Service.Insole3Binder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mInsole3Service = null;
        }
    };


    private void bindService() {
        boolean mBound = false;
        Log.e("MOTICON", "Step 1");
        Intent mIntentInsole3 = new Intent(mainContext, Insole3Service.class);
        Log.e("MOTICON", "Step 2");

        try {
            if (mBound) {
                //Already running
            } else {
                //ContextCompat.startForegroundService(this, mIntentInsole3);


                mainContext.startService(mIntentInsole3);
                //startService(mIntentInsole3);
                mainContext.bindService(mIntentInsole3, mServiceConnection, 0);
                mBound = true;
                Log.e("MOTICON", "Start the service");
            }
        } catch (IllegalStateException e) {
            Log.e("ERROR", e.toString());
        }
    }

    private void unbindService(View v) {
        boolean mBound = false;

        try {
            unbindService(mServiceConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendProtoToService(byte[] message) {

        Intent broadcast = new Intent(Insole3Service.BROADCAST_CONTROLLER_MSG);
        broadcast.putExtra(Insole3Service.EXTRA_PROTO_MSG, message);
        boolean b = LocalBroadcastManager.getInstance(mainContext).sendBroadcast(broadcast);
        Log.d("MESSAGEOUT", String.valueOf(b) + " MSG = " + message.toString());

        //getInstance(getApplicationContext()).sendBroadcast(broadcast);


    }


    class MSGSENDER extends AsyncTask<Void, Void, Void> {

        public MSGSENDER() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                while (true) {
                    Log.d("MESSAGEOUT"," MSG = " );
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    STARTINCOLESCONNECTION = true;
                    while (!STARTINCOLESCONNECTION) {
                        try {
                            Thread.sleep(500);
                            Log.d("[I N F O]", "Waiting for EDGE to start...");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    Log.d("[I N F O]", "STARTING THE CONNECTION");

                    Service.MoticonMessage.Builder moticonMessage1 = Service.MoticonMessage.newBuilder();
                    Service.StartInsoleScan sis = Service.StartInsoleScan.newBuilder().setIncludeDFU(false).build();
                    moticonMessage1.setStartInsoleScan(sis);

                    Service.MoticonMessage moticonMessage3 = Service.MoticonMessage.parseFrom(moticonMessage1.build().toByteArray());
                    Log.d("Constructed frame ADV", moticonMessage3.toString());


                    //CONNECTED = true;
                    while (!CONNECTED) {

                        try {
                            Thread.sleep(1000);
                            Log.d("[I N F O]", "WAITING FOR CONNECTION");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        sendProtoToService(moticonMessage1.build().toByteArray());
                    }
                    READYTOSTARTSERVICE = true;
                    while (!READYTOSTARTSERVICE) {

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    //Log.d("S E R I A L ", String.valueOf(insoleserial));

                    //READYTOSTARTSERVICE = true;

                    Date date = new Date();
                    Service.MoticonMessage.Builder moticonMessage2 = Service.MoticonMessage.newBuilder();
                    Common.ServiceId sid = Common.ServiceId.newBuilder().setLeftSerialNumber(insoleserialLEFT).setRightSerialNumber(insoleserialRIGHT).build();
                    Common.ServiceType stp = Common.ServiceType.LIVE;


                    Common.ServiceEndpoint sep = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointType(Common.ServiceEndpoint.EndpointType.APP).
                            build();

                    Common.EndpointSettings eps = Common.EndpointSettings.newBuilder().
                            setIpAddress(ip).
                            setPort(9083).
                            build();

                    Common.ServiceEndpoint sep2 = Common.ServiceEndpoint.
                            newBuilder().
                            setEndpointSettings(eps).
                            setEndpointType(Common.ServiceEndpoint.EndpointType.PC).
                            build();

                    Common.ServiceConfig.AccGRange accr = Common.ServiceConfig.AccGRange.ACC_16_G;
                    Common.ServiceConfig.AccOdr accor = Common.ServiceConfig.AccOdr.ACC_104_ODR;
                    Common.ServiceConfig.AngOdr angor = Common.ServiceConfig.AngOdr.ANG_104_ODR;

                    List<Boolean> myPressure = new ArrayList<Boolean>();
                    for (int p = 0; p < 16; p++)
                        myPressure.add(true);

                    List<Boolean> myAngular = new ArrayList<Boolean>();
                    for (int p = 0; p < 3; p++)
                        myAngular.add(true);

                    List<Boolean> myAcceler = new ArrayList<Boolean>();
                    for (int p = 0; p < 3; p++)
                        myAcceler.add(true);

                    List<Boolean> myCop = new ArrayList<Boolean>();
                    for (int p = 0; p < 2; p++)
                        myCop.add(true);


                    Common.ServiceConfig tmpserviceconf = Common.ServiceConfig.newBuilder().
                            setServiceStartTime(date.getTime()).
                            setServiceId(sid).
                            setAccGRange(accr).
                            setAngDpsRange(Common.ServiceConfig.AngDpsRange.ANG_2000_DPS).
                            addAllEnabledAcceleration(myAcceler).
                            addAllEnabledCop(myCop).
                            addAllEnabledPressure(myPressure).
                            addAllEnabledAngular(myAngular).
                            setEnabledPressure(15, true).
                            setEnabledAcceleration(2, true).
                            setEnabledAngular(2, true).
                            setEnabledCop(1, true).
                            setEnabledTotalForce(true).
                            setServiceType(stp).
                            setRate(100).
                            setAccOdr(accor).
                            setAngOdr(angor).
                            setEnabledTotalForce(true).
                            setActivityProfile(Common.ServiceConfig.ActivityProfile.ACTIVITY_PROFILE_CONTINUOUS).
                            build();





                    Service.StartService msgStartService = Service.StartService.
                            newBuilder().
                            setServiceConfig(tmpserviceconf).
                            setServiceEndpoint(sep).
                            build();

                    moticonMessage2.setStartService(msgStartService);


                    Service.MoticonMessage tmp;
                    tmp = Service.MoticonMessage.parseFrom(moticonMessage2.build().toByteArray());
                    Log.d("test", tmp.toString());

                    sendProtoToService(moticonMessage2.build().toByteArray());


                    STARTINCOLESCONNECTION = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void sendProtoToService(byte[] message) {

            Intent broadcast = new Intent(Insole3Service.BROADCAST_CONTROLLER_MSG);
            broadcast.putExtra(Insole3Service.EXTRA_PROTO_MSG, message);
            boolean b = LocalBroadcastManager.getInstance(mainContext).sendBroadcast(broadcast);
            Log.d("MESSAGEOUT", String.valueOf(b) + " MSG = " + message.toString());
        }

    }


    class UDPRECEIVER extends AsyncTask<Void, Void, Void> {


        public UDPRECEIVER() {

        }

        public boolean getSTARTINCOLESCONNECTION() {
            return STARTINCOLESCONNECTION;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                Log.e("MSG", "in the UDPreceiver");

                socket = new MulticastSocket(10000);
                InetAddress group = InetAddress.getByName("224.3.29.71");
                socket.joinGroup(group);

                while (true) {

                    //STARTINCOLESCONNECTION = true;

                    WifiManager wifi = (WifiManager) mainContext.getSystemService(Context.WIFI_SERVICE);
                    WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
                    multicastLock.setReferenceCounted(true);
                    multicastLock.acquire();

                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    if (multicastLock != null) {
                        multicastLock.release();
                        multicastLock = null;
                    }
                    String received = new String(
                            packet.getData(), 0, packet.getLength());
                    Log.e("MSG", received);

                    if ("end".equals(received)) {
                        break;
                    }

                    String[] dr = received.split(",");
                    Log.e("udp message", dr[0]);
                    if ("startConnection".equals(dr[0])) {
                        STARTINCOLESCONNECTION = true;
                        ip = dr[1];
                    }


                    if ("endService".equals(dr[0])) {
                        Service.StopServiceConf ssc = Service.StopServiceConf.newBuilder().build();

                        Service.StopService stopmyservice = Service.StopService.newBuilder().setRightServiceCounter(RIGHTSERVICECOUNTER).setLeftServiceCounter(LEFTSERVICECOUNTER).build();
                        Service.MoticonMessage.Builder moticonMessage5 = Service.MoticonMessage.newBuilder();
                        moticonMessage5.setStopService(stopmyservice);
                        sendProtoToService(moticonMessage5.build().toByteArray());
                        Log.d("M o t i c o n] ", "Ending service");

                        Service.DisconnectInsoles din = Service.DisconnectInsoles.newBuilder().build();
                        Service.MoticonMessage.Builder moticonMessage6 = Service.MoticonMessage.newBuilder();
                        moticonMessage6.setDisconnectInsoles(din);
                        sendProtoToService(moticonMessage6.build().toByteArray());
                        STARTINCOLESCONNECTION = false;
                        CONESTABLISHED = false;
                        CONNECTED = false;
                        READYTOSTARTSERVICE = false;
                        RIGHTSERVICECOUNTER = 0;
                        FOUNDLEFT = false;
                        FOUNDRIGHT = false;
                        CON_LEFT_ESTABLISHED = false;
                        CON_RIGHT_ESTABLISHED = false;
                    }
                }
                //}
                socket.leaveGroup(group);
                socket.close();


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void sendProtoToService(byte[] message) {

            Intent broadcast = new Intent(Insole3Service.BROADCAST_CONTROLLER_MSG);
            broadcast.putExtra(Insole3Service.EXTRA_PROTO_MSG, message);
            boolean b = LocalBroadcastManager.getInstance(mainContext).sendBroadcast(broadcast);
            Log.d("MESSAGEOUT", String.valueOf(b) + " MSG = " + message.toString());

            //getInstance(getApplicationContext()).sendBroadcast(broadcast);


        }


    }
}
