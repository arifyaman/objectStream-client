package com.xlip.objectstream.client;

import com.xlip.objectstream.communication.LockedWrap;
import com.xlip.objectstream.communication.Wrap;
import com.xlip.objectstream.communication.service.EncryptionService;
import com.xlip.objectstream.communication.sub.WrapType;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

public class Client extends Thread {
    private Socket socket = null;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private boolean connected;

    @Setter
    @Getter
    private ClientCallbacks clientCallbacks;


    public Client(String host) {

        try {
            socket = new Socket(host, 31994);
            this.outputStream = new ObjectOutputStream(socket.getOutputStream());
            this.inputStream = new ObjectInputStream(socket.getInputStream());
            this.connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            this.connected = false;
        }


    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        while (isConnected()) {
            try {
                Object o = inputStream.readObject();
                Wrap wrap = null;

                if (o instanceof LockedWrap) {
                    wrap = EncryptionService.getInstance().resolveWrap(((LockedWrap) o).getBytes());

                } else if (o instanceof Wrap) {
                    wrap = ((Wrap) o);
                }

                Client.this.processWrap(wrap);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                    this.clientCallbacks.disconnected();
                    break;
                } catch (IOException x) {
                    x.printStackTrace();
                    break;
                }
            } catch (Exception e2) {
                if (e2 instanceof BadPaddingException || e2 instanceof IllegalBlockSizeException) {
                    close();
                }
                e2.printStackTrace();

            }

        }

    }

    public void close() {
        this.connected = false;
        disconnect();
        interrupt();
    }

    @Override
    public void run() {
        read();
    }

    public void processWrap(Wrap wrap) {
        if (wrap.getWrapType() == WrapType.REQUEST) {
            if (wrap.getCmd().equals("PREPARE")) {
                HashMap<String, String> encMap = ((HashMap<String, String>) wrap.getPayload());
                EncryptionService.init(encMap.get("iv"), encMap.get("key"));
                register();
            } else if (wrap.getCmd().equals("CHANGE_ENC")) {
                HashMap<String, String> encMap = ((HashMap<String, String>) wrap.getPayload());
                EncryptionService.init(encMap.get("iv"), encMap.get("key"));
                System.out.println("Enc changed");
            }
        } else {
            this.clientCallbacks.wrapReceived(wrap);
        }
    }

    public void register() {
        Wrap registerWrap = Wrap.createRequest();
        registerWrap.setCmd("REGISTER");

        dispatchWrap(registerWrap);
    }


    public void dispatchWrap(Wrap wrap) {
        try {
            if (EncryptionService.instance.isInitialized()) {
                LockedWrap lockedWrap = EncryptionService.getInstance().lockWrap(wrap);
                outputStream.writeObject(lockedWrap);
                return;
            }

            outputStream.writeObject(wrap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public interface ClientCallbacks {
        void wrapReceived(Wrap wrap);

        void disconnected();

    }
}
