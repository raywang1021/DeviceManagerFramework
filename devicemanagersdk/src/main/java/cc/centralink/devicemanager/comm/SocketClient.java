package cc.centralink.devicemanager.comm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import static java.lang.Thread.sleep;

/**
 * Created by kclin on 6/1/15.
 */
public class SocketClient implements Runnable {


    private String serverMessage;
    public static final String SERVERIP = "127.0.0.1"; //your computer IP address
    public static final int SERVERPORT = 6004;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    private boolean mIsConnected = false;
    private Socket mSocket = null;

    private Handler mHandler;

    PrintWriter out;
    BufferedReader in;

    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public SocketClient(OnMessageReceived listener) {
        mMessageListener = listener;
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        try {
                            mMessageListener.messageReceived((String)msg.obj);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            Crashlytics.logException(e);
                        }
                        break;
                    default:
                        Crashlytics.log("Received unknown message from SocketClient.");
                }
            }
        };
    }


    /**
     * Sends the message entered by client to the server
     * @param message text entered by client
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.println(message);
            out.flush();
        }
    }

    public void stopClient(){
        mRun = false;
    }

    public boolean isConnected() {

        if (out == null) {
            return false;
        } else {
            return !out.checkError() && mIsConnected;
        }
    }

    public void run() {

        Log.e("SocketClient", "run");

        mRun = true;

        while (mRun) {
            try {
                //here you must put your computer's IP address.
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);

                Log.e("TCP Client", "C: Connecting..." + Thread.currentThread().getName());

                //create a socket to make the connection with the server
                mSocket = new Socket(serverAddr, SERVERPORT);
                mSocket.setKeepAlive(true);

                if (mSocket.isConnected()) {
                    Log.e("TCP Client", "Socket is connected.");
                    mIsConnected = true;
                } else {
                    Log.e("TCP Client", "Socket is not connected, wait for 1s to retry.");
                    sleep(1000);
                    continue;
                }

                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);

                Log.e("TCP Client", "C: Done.");

                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                while (mRun) {
                    serverMessage = in.readLine();

                    if (serverMessage == null) {
                        mIsConnected = false;
                        throw new IOException("Socket is closed by remote peer.");
                    }

                    if (mMessageListener != null) {
                        // FIXME
                        // workaround to resolve the issue about if two CMD come from the same readline()
                        // if we don't handle it, it will cause lock issue for UI for getting wrong device list.
                        if (!serverMessage.contains("@")) {
                            Message msg = Message.obtain();
                            msg.what = 0;
                            msg.obj = new String(serverMessage);
                            mHandler.sendMessage(msg);
                        }
                        else {
                            String[] splitCMDMessage = serverMessage.split("@");
                            for (int i = 1; i < splitCMDMessage.length; ++i) {
                                if (i > 1) {
                                    Log.e("TCP Client", "Multiple CMD message come with one read");
                                }

                                Message msg = Message.obtain();
                                msg.what = 0;
                                msg.obj = new String(splitCMDMessage[i]);
                                mHandler.sendMessage(msg);
                            }
                        }
                    }
                    else {
                        sleep(1000, 0);
                    }
                    serverMessage = null;
                }
            } catch (Throwable t) {
                Crashlytics.logException(t);
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                try {
                    mSocket.close();
                } catch (Throwable t) {
                    Crashlytics.logException(t);
                    t.printStackTrace();
                }
            }
        }

    }


    public interface OnMessageReceived {
        public void messageReceived(String message) throws RemoteException;
    }

}
