// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package samples.com.microsoft.azure.sdk.iot;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Pair;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


/** Sends a number of event messages to an IoT Hub. */
public class SendEvent
{
    static int sentMessageCount = 0;
    static int ackedMessageCount = 0;
    final static int numberOfMessagesToSend = 500;

    static int messageSizeInBytes = 1; // 1 byte
    //static int messageSizeInBytes = 1024; //1 kilobyte
    //static int messageSizeInBytes = 1024 * 32; //32 kilobytes
    //static int messageSizeInBytes = 1024 * 64; //64 kilobytes
    //static int messageSizeInBytes = 1024 * 128; //128 kilobytes
    //static int messageSizeInBytes = 1024 * 255; //255 kilobytes (Max message size allowed for d2c telemetry)

    static CountDownLatch countDownLatch;

    static DeviceClient client;

    static double[] startTimes = new double[numberOfMessagesToSend];
    static double[] stopTimes = new double[numberOfMessagesToSend];


    private static class SendEventRunnable implements java.lang.Runnable
    {
        private Message messageToSend;
        private EventCallback eventCallback;
        private int messageIndex;

        public SendEventRunnable(Message messageToSend, EventCallback eventCallback, int messageIndex)
        {
            this.messageToSend = messageToSend;
            this.eventCallback = eventCallback;
            this.messageIndex = messageIndex;
        }

        @Override
        public void run()
        {
            startTimes[messageIndex] = System.currentTimeMillis();
            client.sendEventAsync(this.messageToSend, this.eventCallback, this.messageIndex);
        }
    }

    protected static class EventCallback implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            //stop time
            long currentTime = System.currentTimeMillis();
            int messageIndex = (int) context;
            stopTimes[messageIndex] = currentTime;


            ackedMessageCount++;
            countDownLatch.countDown();
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException
    {
        String connString = "<device connection string for a device in your B3 or S3 iot hub>";
        client = new DeviceClient(connString, IotHubClientProtocol.MQTT);

        long clientSendInterval = 10; //Lower number here spawns send threads more frequently, can send more quickly. By default, value is 10
        client.setOption("SetSendInterval", clientSendInterval);
        countDownLatch = new CountDownLatch((int) numberOfMessagesToSend);

        System.out.println("Message size in bytes per send: " + messageSizeInBytes);

        byte[] body = new byte[messageSizeInBytes];
        for (int i = 0; i < messageSizeInBytes; i++)
        {
            body[i] = 1;
        }

        final SendEventRunnable[] sendEventRunnables = new SendEventRunnable[numberOfMessagesToSend];
        for (int messageIndex = 0; messageIndex < numberOfMessagesToSend; messageIndex++)
        {
            Message msg = new Message(new String(body.clone()));
            sendEventRunnables[messageIndex] = new SendEventRunnable(msg, new EventCallback(), messageIndex);
        }

        client.open();

        //start up all sender threads
        final long overallStartTime = System.currentTimeMillis();
        for (int sentMessageCount = 0; sentMessageCount < numberOfMessagesToSend; sentMessageCount++)
        {
            new Thread(sendEventRunnables[sentMessageCount]).start();
        }

        //wait until all sent messages have been acknowledged by the iot hub, or until 90 minutes have passed
        countDownLatch.await(90, TimeUnit.MINUTES);

        final long overallStopTime = System.currentTimeMillis();
        double secondsTaken = ((overallStopTime - overallStartTime) / 1000.0);
        System.out.println("Overall time taken: " + secondsTaken + " seconds");


        System.out.println("Average seconds between send and ack per message: " + calculateAverageSecondsBetweenSendAndAck());

        double messagesPerSecond = ackedMessageCount / secondsTaken;
        System.out.println("Messages per second: " + messagesPerSecond);

        client.closeNow();
    }

    private static double calculateAverageSecondsBetweenSendAndAck()
    {
        double averageTimeTakenPerMessage = 0;
        for (int messageIndex = 0; messageIndex < numberOfMessagesToSend; messageIndex++)
        {
            double secondsTakenOnMessage = (stopTimes[messageIndex] - startTimes[messageIndex]) / 1000;
            averageTimeTakenPerMessage += secondsTakenOnMessage;
        }

        return averageTimeTakenPerMessage / numberOfMessagesToSend;
    }
}
