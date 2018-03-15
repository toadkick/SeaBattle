package seaBattle.xmlservice;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.net.Socket;

/**
 * <code>OutServerXML</code>
 * provides sending keys/values with XMLStreamWriter
 */

public class OutServerXML {
    private XMLOutputFactory factory;
    private XMLStreamWriter writer;
    private Socket socket;

    public OutServerXML(Socket socket){
        this.socket = socket;
        factory = XMLOutputFactory.newInstance();
        try {
            writer = factory.createXMLStreamWriter(socket.getOutputStream());
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String key, String value){
        try {
        writer.writeStartDocument("1.0");
        writer.writeStartElement("root");
        writer.writeStartElement("key");
        writer.writeCharacters(key);
        writer.writeEndElement();
        writer.writeStartElement("value");
        writer.writeCharacters(value);
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    public void send(String key,String shootResult, String value1, String value2){
        try {
            writer.writeStartDocument("1.0");
            writer.writeStartElement("root");
            writer.writeStartElement("key");
            writer.writeCharacters(key);
            writer.writeEndElement();
            writer.writeStartElement("value");
            writer.writeCharacters(shootResult);
            writer.writeEndElement();
            writer.writeStartElement("value");
            writer.writeCharacters(value1);
            writer.writeEndElement();
            writer.writeStartElement("value");
            writer.writeCharacters(value2);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    //XML with many values
    public void send(String key, String[] list) {
        try {
        writer.writeStartDocument("1.0");
        writer.writeStartElement("root");
        writer.writeStartElement("key");
        writer.writeCharacters(key);
        writer.writeEndElement();

            for (String aList : list) {
                writer.writeStartElement("value");
                writer.writeCharacters(aList);
                writer.writeEndElement();
            }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    //getters & setters
    public XMLOutputFactory getFactory() {
        return factory;
    }

    public void setFactory(XMLOutputFactory factory) {
        this.factory = factory;
    }

    public XMLStreamWriter getWriter() {
        return writer;
    }

    public void setWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }
}
