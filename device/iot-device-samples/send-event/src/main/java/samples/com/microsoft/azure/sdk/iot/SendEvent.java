// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package samples.com.microsoft.azure.sdk.iot;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/** Sends a number of event messages to an IoT Hub. */
public class SendEvent
{
    static int sentMessageCount = 0;
    static int ackedMessageCount = 0;

    static CountDownLatch countDownLatch;

    protected static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            ackedMessageCount++;
            countDownLatch.countDown();
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException
    {
        String connString = "<device connection string for a device in your B3 or S3 iot hub>";

        final int numberOfMessagesToSend = 5000;
        int messageSizeInBytes = 1024; //1 kilobyte
        //int messageSizeInBytes = 1024 * 64; //64 kilobytes
        //int messageSizeInBytes = 1024 * 1024; //1 megabyte
        //int messageSizeInBytes = 1024 * 1024 * 1024; //1 gigabyte

        byte[] body = new byte[messageSizeInBytes];
        for (int i = 0; i < messageSizeInBytes; i++)
        {
            body[i] = 1;
        }

        final Message[] messagesToSend = new Message[numberOfMessagesToSend];
        for (int messageIndex = 0; messageIndex < numberOfMessagesToSend; messageIndex++)
        {
            messagesToSend[messageIndex] = new Message(new String(body.clone()));
        }

        final IotHubEventCallback[] callbacks = new IotHubEventCallback[numberOfMessagesToSend];
        for (int messageIndex = 0; messageIndex < numberOfMessagesToSend; messageIndex++)
        {
            callbacks[messageIndex] = new EventCallback();
        }

        DeviceClient client = new DeviceClient(connString, IotHubClientProtocol.MQTT);
        long clientSendInterval = 5; //Lower number here spawns send threads more frequently, can send more quickly. By default, value is 10
        client.setOption("SetSendInterval", clientSendInterval);
        countDownLatch = new CountDownLatch((int) numberOfMessagesToSend);

        client.open();

        final long startTime = System.currentTimeMillis();
        for (int sentMessageCount = 0; sentMessageCount < numberOfMessagesToSend; sentMessageCount++)
        {
            Message message = messagesToSend[sentMessageCount];
            client.sendEventAsync(message, callbacks[sentMessageCount], message);
        }

        //wait until all sent messages have been acknowledged by the iot hub, or until 90 minutes have passed
        countDownLatch.await(90, TimeUnit.MINUTES);

        final long stopTime = System.currentTimeMillis();
        double secondsTaken = ((stopTime - startTime) / 1000.0);
        System.out.println("Time taken: " + secondsTaken + " seconds");

        double messagesPerSecond = ackedMessageCount / secondsTaken;
        System.out.println("Messages per second: " + messagesPerSecond);

        client.closeNow();
    }
}
