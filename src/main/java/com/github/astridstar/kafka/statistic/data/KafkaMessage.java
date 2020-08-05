package com.github.astridstar.kafka.statistic.data;

import com.github.astridstar.kafka.statistic.loggers.GeneralLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.StringTokenizer;

public class KafkaMessage {
    public String messageId_ = "-1";
    public int sourceId_ = -1;
    public long timestamp_ = -1;
    public String topic_ = "";
    public byte[] content_ = null;
    private static boolean bToExtractContent_ = Configurator.getConsumerDestinationFolder().isBlank() ? false : true;

    protected char separator_ = '|';

    public KafkaMessage()
    {

    }

    public KafkaMessage(byte[] rawData) throws ParseException, NullPointerException
    {
        deserialize(rawData);
    }

    public KafkaMessage(String msgId, int srcId, long time, String topic, byte[] payload)
    {
        messageId_ = msgId;
        sourceId_ = srcId;
        timestamp_ = time;
        topic_ = topic;
        content_ = payload;
    }

    public void log()
    {
        GeneralLogger.getDefaultLogger().info(String.format("[KAFKA_M]|MsgId[%s]|Producer[%d]|Timestamp[%d]|TopicName[%s]|Content[%d]Bytes",
                messageId_, sourceId_, timestamp_, topic_, (content_ == null) ? 0 : content_.length));
    }

    public String getString()
    {
        return String.format("[KAFKA_M]|MsgId[%s]|Producer[%d]|Timestamp[%d]|TopicName[%s]|Content[%d]Bytes",
                messageId_, sourceId_, timestamp_, topic_, (content_ == null) ? 0 : content_.length);
    }

    public long calculateSizeInBytes() {

        String header = String.format("%s%s%d%s%d%s%s%s",
                messageId_, separator_, sourceId_, separator_, timestamp_, separator_, topic_, Configurator.DEFAULT_PAYLOAD_MARKER);

        return (header.length() + (content_ == null ? 0 : content_.length)) + 1;
    }

    public byte[] serialize()
    {
		/*
		return String.format("%s%s%d%s%d%s%s%s%s",
				messageId_, separator_, sourceId_, separator_, timestamp_, separator_, topic_,
				Configurator.DEFAULT_PAYLOAD_MARKER, content_);
				*/
        String header = String.format("%s%s%d%s%d%s%s%s",
                messageId_, separator_, sourceId_, separator_, timestamp_, separator_, topic_, Configurator.DEFAULT_PAYLOAD_MARKER);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(header.getBytes(StandardCharsets.UTF_8));

            if(content_ != null && content_.length > 0)
                output.write(content_);

            output.write('\n');

            return output.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            GeneralLogger.getDefaultLogger().error("Unable to serialize message." + e.getMessage());
            return null;
        }

    }

    public void deserialize(byte[] rawData) throws ParseException
    {
        int position = -1;
        boolean bPayloadMarker = false;
        for(byte c : rawData) {
            position++;
            if(c == Configurator.DEFAULT_PAYLOAD_MARKER.charAt(0)) {
                bPayloadMarker = true;
                break;
            }
        }

        // No marker found, strange but treat it as no content....
        if(!bPayloadMarker) {
            GeneralLogger.getDefaultLogger().warn("[STRANGE] Unable to find content marker.  Treating the whole message as the header content");
            position = rawData.length - 1;
        }

        // De-serialize the header
        byte[] header = Arrays.copyOfRange( rawData, 0, position); // Remove the marker
        deserializeHeader(header);

        if(!bPayloadMarker || (position == rawData.length -1)) {
            content_ = null;
            return;
        }

        // De-serialize the content and extract to file
        content_ = Arrays.copyOfRange(rawData, position + 1, rawData.length -1);
        deserializeContent();
    }

    private void deserializeHeader(byte[] header) throws ParseException {

        String s = new String(header, StandardCharsets.UTF_8); //Base64.getEncoder().encodeToString(header);
        StringTokenizer tokenizer = new StringTokenizer(s, "|");

        if (tokenizer.countTokens() < 4) {
            throw new ParseException(
                    String.format("Unable to deserialize content. Incorrect number of elements. Expecting 4 only but getting [%d]",
                            tokenizer.countTokens()),
                    tokenizer.countTokens());
        }

        try {
            messageId_ = tokenizer.nextToken();
            sourceId_ = Integer.parseInt(tokenizer.nextToken());
            timestamp_ = Long.parseLong(tokenizer.nextToken());
            topic_ = tokenizer.nextToken();
        } catch(NumberFormatException e) {
            e.printStackTrace();
            throw new ParseException("Unable to deserialize header content. Number format errors in the header", tokenizer.countTokens());
        }
    }

    private void deserializeContent() {
        extractContentToFile();
    }

    private void extractContentToFile()
    {
        if(content_ == null)
            return;

        // Read
        if(bToExtractContent_ && content_.length > 0) {
            try {
                FileOutputStream fos=new FileOutputStream(Configurator.getConsumerDestinationFolder() + File.separator + messageId_);
                fos.write(content_);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
