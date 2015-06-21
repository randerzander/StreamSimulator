package com.github.randerzander;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.util.Properties;
import java.text.SimpleDateFormat;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class StreamSimulator {
	public static void main(String[] args) {
    if (args.length < 4 ){
      System.out.println("Usage: kafka_topic input brokerhost speed [delimiter] [timestamp_col] [\"dateformat\"]");
      System.out.println("dateformat is yyyy/MM/dd HH:mm:ss.SSS by default");
      System.exit(-1);
    }
    String topic = args[0];
    String input = args[1];
    String host = args[2];
    if (!host.contains(":")) host = host + ":6667";
    int speed = Integer.parseInt(args[3]);
    SimpleDateFormat dateFormat = (args.length == 7) ? new SimpleDateFormat(args[6]) : new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    Properties props = new Properties();
    props.put("metadata.broker.list", host); //HDP 2.2 Kafka uses port 6667 by default
    props.put("serializer.class", "kafka.serializer.StringEncoder");
    props.put("request.required.acks", "1"); //Using synchronous message production
    ProducerConfig config = new ProducerConfig(props);
    Producer<String, String> producer = new Producer<String, String>(config);
    File[] files = new File(input).listFiles();

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
          System.out.println("Sending: " + topic+":"+host+":" + line);
          producer.send(new KeyedMessage<String, String>(topic, line));
        }
        System.out.println("Finished ingesting file: " + path);
        br.close();
      }catch (Exception e){
        e.printStackTrace();
        System.exit(-1);
      }
    }
    producer.close();
    System.exit(0);
	}
}
