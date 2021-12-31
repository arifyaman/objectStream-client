package com.xlip.objectstream.client;

import com.xlip.objectstream.communication.Wrap;
import com.xlip.objectstream.communication.sub.ExecutableClass;

import java.util.Scanner;

public class Main implements Client.ClientCallbacks {

    public Main() {
        Client client = new Client("localhost");
        client.setClientCallbacks(this);
        client.start();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String line = scanner.nextLine();
            ExecutableClass executableClass = new ExecutableClass();

            client.dispatchWrap(Wrap.builder().cmd(line).build());
        }
    }

    public static void main(String[] args) {
         new Main();
    }

    @Override
    public void wrapReceived(Wrap wrap) {
        System.out.println(wrap);
    }

    @Override
    public void disconnected() {

    }
}
