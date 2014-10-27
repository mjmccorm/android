package com.example.mjmccorm.flightsensors;

/**
 * Created by mjmccorm on 10/26/2014.
 */
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class UDPThread  {

    String msensordata;

    public UDPThread(String sensordata) {
        this.msensordata = sensordata;
    }

    public void send()
    {
        byte bytes [] ;

        try {
            bytes = msensordata.getBytes("UTF-8");
            if (ActivityCollection.mPacket == null || ActivityCollection.mSocket == null)
                return ;

            ActivityCollection.mPacket.setData(bytes);
            ActivityCollection.mPacket.setLength(bytes.length);
            ActivityCollection.mSocket.send(ActivityCollection.mPacket);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //Log.e("Error", "SendBlock");
            return ;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //Log.e("Error", "SendBlock");
            return ;
        }

    }
}