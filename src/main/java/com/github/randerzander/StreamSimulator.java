package com.github.randerzander;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;

// Kafka imports
/*
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
*/

// NiFi imports
import org.apache.nifi.remote.Transaction;
import org.apache.nifi.remote.TransactionCompletion;
import org.apache.nifi.remote.TransferDirection;
import org.apache.nifi.remote.client.SiteToSiteClient;
import org.apache.nifi.remote.protocol.SiteToSiteTransportProtocol;
import org.apache.nifi.remote.protocol.DataPacket;

public class StreamSimulator {
	public static void main(String[] args) {
    if (args.length < 4 ){
      System.out.println("Usage: input_dir host topic/portname speed [delimiter] [timestamp_col] [\"dateformat\"]");
      System.out.println("dateformat is yyyy/MM/dd HH:mm:ss.SSS by default");
      System.exit(-1);
    }
    String input = args[0];
    String host = args[1];
    String topic = args[2];
    if (!host.contains(":")) host = host + ":6667";
    int speed = Integer.parseInt(args[3]);
    SimpleDateFormat dateFormat = (args.length == 7) ? new SimpleDateFormat(args[6]) : new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    /*
    Properties props = new Properties();
    props.put("metadata.broker.list", host); //HDP 2.2 Kafka uses port 6667 by default
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("request.required.acks", "1"); //Using synchronous message production
    ProducerConfig config = new ProducerConfig(props);
    Producer<String, String> producer = new Producer<String, String>(config);
    */
    File[] files = new File(input).listFiles();

    SiteToSiteClient.Builder builder = new SiteToSiteClient.Builder();
		builder.url("http://"+host+"/nifi");
		builder.portName(topic);
    builder.transportProtocol(SiteToSiteTransportProtocol.HTTP);
		builder.nodePenalizationPeriod(60, TimeUnit.SECONDS);
		SiteToSiteClient client = builder.build();


    //For each file in the directory, read it and feed it to Kafka line by line
    for(File file : files){
      try{
        String path = input + file.getName();
        BufferedReader br = new BufferedReader(new InputStreamReader((path.contains(".gz")) ?
          new GZIPInputStream(new FileInputStream(path)) :
          new FileInputStream(path)
        ));

        String line;
        long prevTimestamp = 0l;
        while ((line = br.readLine()) != null) {
          if (speed == -1){
            //Split line into columns to extract a timestamp
            String ts = line.split(args[4])[Integer.parseInt(args[5])];
            //Attempt to parse into date time and compute time delta from last timestamp
            long currentTimestamp = dateFormat.parse(ts).getTime();
            //Sleep for the delta
            if (prevTimestamp != 0) Thread.sleep(currentTimestamp - prevTimestamp);
            prevTimestamp = currentTimestamp;
          }else if (speed > 0) Thread.sleep(speed);
          System.out.println("Sending: " + topic +":"+host+":" + line);
          //producer.send(new KeyedMessage<String, String>(topic, line));
          HashMap<String,String> attributes = new HashMap<String,String>();
          try{
            Transaction transaction = client.createTransaction(TransferDirection.SEND);
            transaction.send(new SimplePacket(line, attributes));
            transaction.confirm();
            TransactionCompletion completion =  transaction.complete();
            System.out.println("Bytes transferred: " + completion.getBytesTransferred());
          }catch(Exception e){
            e.printStackTrace(System.out);
          }
        }
        System.out.println("Finished ingesting file: " + path);
        br.close();
      }catch (Exception e){
        e.printStackTrace();
        System.exit(-1);
      }
    }
    //producer.close();
    System.exit(0);
	}

  private static class SimplePacket implements DataPacket{
    private final InputStream input;
    private final long size;
    private final Map<String,String> attributes;
    
    public SimplePacket(final String data, final Map<String,String> attributes) throws IOException{
      if(data!=null){
        this.size = data.length();
        this.input= new ByteArrayInputStream(data.getBytes());
      }else{
        this.size=0;
        this.input = new ByteArrayInputStream(new byte[0]);
      }
      
      if(attributes==null)
        this.attributes = new HashMap<String,String>();
      else
        this.attributes=attributes;
    }

    public Map<String, String> getAttributes() {
      return this.attributes;
    }

    public InputStream getData() {
      return this.input;
    }

    public long getSize() {
      return this.size;
    }
  }
}
