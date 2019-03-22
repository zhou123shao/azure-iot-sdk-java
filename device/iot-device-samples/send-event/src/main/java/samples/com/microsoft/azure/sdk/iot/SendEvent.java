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
        final long messagesToSend = 1000000;
        String connString = "<device connection string for a device in your B3 or S3 iot hub>";
        DeviceClient client = new DeviceClient(connString, IotHubClientProtocol.MQTT);
        long clientSendInterval = 10; //Lower number here spawns send threads more frequently, can send more quickly. By default, value is 10
        client.setOption("SetSendInterval", clientSendInterval);
        countDownLatch = new CountDownLatch((int) messagesToSend);

        client.open();

        final long startTime = System.currentTimeMillis();
        while (sentMessageCount < messagesToSend)
        {
            Message message = new Message("a");
            EventCallback callback = new EventCallback();
            client.sendEventAsync(message, callback, message);
            sentMessageCount++;
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
