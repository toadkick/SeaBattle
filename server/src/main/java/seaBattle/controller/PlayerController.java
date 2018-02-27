package seaBattle.controller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import seaBattle.model.Player;
import seaBattle.model.Server;
import seaBattle.xmlservice.InServerXML;
import seaBattle.xmlservice.OutServerXML;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.Socket;

public class PlayerController extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int threadNumber;
    private static final String PLAYERLIST = "playerList.xml";
    private String str;
    private InServerXML inServerXML;
    private OutServerXML outServerXML;
    private Player player;

    public PlayerController(Socket socket) throws IOException {
        player = new Player();
        this.socket = socket;
        inServerXML = new InServerXML(socket);
        outServerXML = new OutServerXML(socket);
    }

    public void run() {
        threadNumber = Server.getCountOfThread();
        try {
            while (!socket.isClosed()){
                System.out.println("waiting to receive xml data in thread #" + threadNumber);
                //for XML data receiving
                inServerXML.setReader(inServerXML.getFactory().createXMLStreamReader(inServerXML.getFileReader()));
                XMLStreamReader reader = inServerXML.getReader();
                while (reader.hasNext()) {
                    if (reader.getEventType() == 1 && reader.getLocalName().equals("key")){
                        reader.next();
                        switch (reader.getText()){
                            case "AUTHORIZATION": {
                                System.out.println("\nxml message with key \"AUTHORIZATION\" from thread #" + threadNumber + " detected\npreparing answer:");
                                reader.next();
                                reader.next();
                                reader.next();
                                player.setLogin(reader.getText());
                                reader.next();
                                reader.next();
                                reader.next();
                                player.setPassword(reader.getText());
                                outServerXML.send("AUTHORIZATION", authorizationPhase(player.getLogin(),player.getPassword()));
                                break;
                            }
                            case "REGISTRATION":
                                System.out.println("xml message with key \"REGISTRATION\" detected");
                                break;
                            case "MESSAGE":
                                System.out.println("xml message with key \"MESSAGE\" detected");
                                break;
                            case "ASK OUT":
                                System.out.println("xml message with key \"ASK OUT\" detected");
                                break;
                            case "ASK ANSWER":
                                System.out.println("xml message with key \"ASK ANSWER\" detected");
                                break;
                            case "SHIP LOCATION":
                                System.out.println("xml message with key \"SHIP LOCATION\" detected");
                                break;
                            case "READY":
                                System.out.println("xml message with key \"READY\" detected");
                                break;
                            case "SHOOT":
                                System.out.println("xml message with key \"SHOOT\" detected");
                                break;
                            case "SURRENDER":
                                System.out.println("xml message with key \"SURRENDER\" detected");
                                break;
                            case "GAME OVER":
                                System.out.println("xml message with key \"GAME OVER\" detected");
                        }
                    }
                    if (reader.isEndElement() && "root".equals(reader.getName().toString())) {
                        break;
                    }else{
                        reader.next();
                    }
                }

                System.out.println("\nfinished to receive xml data in thread #" + threadNumber);
                reader.close();
            }
        } catch (XMLStreamException | SAXException | ParserConfigurationException | IOException | TransformerException e) {
            e.printStackTrace();
        } finally {

            if (player.getLogin() != null) {
                Server.getNames().remove(player.getLogin());
            }
            if (out != null) {
                Server.getWriters().remove(out);
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }
    
    public String authorizationPhase(String login, String password) throws ParserConfigurationException, IOException, SAXException, TransformerException, XMLStreamException {

        // Строим объектную модель исходного XML файла
        final String filepath = System.getProperty("user.dir") + File.separator + PLAYERLIST;
        final File xmlFile = new File(filepath);
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.parse(xmlFile);
        doc.normalize();

        // бежим по каждому player в players (playerList.xml)
        NodeList players = doc.getElementsByTagName("player");
        for (int i = 0; i < players.getLength(); i++) {

            Node node = players.item(i);
            //если указанные имя и пароль не найдены в списке
            if (!node.getChildNodes().item(1).getTextContent().equals(login) && !node.getChildNodes().item(3).getTextContent().equals(password)){
                str = "***player with this login or password was not found. register first***";
            }

            //если совпадает имя или пароль
            if ((node.getChildNodes().item(1).getTextContent().equals(login) && !node.getChildNodes().item(3).getTextContent().equals(password)) ||
                    (!node.getChildNodes().item(1).getTextContent().equals(login) && node.getChildNodes().item(3).getTextContent().equals(password))){
                str = "***account name or password is incorrect***";
                break;
            }
            //если совпадает и имя и пароль
            if (node.getChildNodes().item(1).getTextContent().equals(login) && node.getChildNodes().item(3).getTextContent().equals(password)) {
                // изменение статуса на online
                node.getChildNodes().item(5).setTextContent("online");

                // Записываем изменения в XML файл
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(filepath));
                transformer.transform(source, result);

                System.out.println("THREAD #" + threadNumber + "." + login + " authorized, status changed to \"online\". moving to IDLE mode with chat");
                str = "***authorization success!***";
                break;
            }
        }
        // Сообщение клиенту о результате авторизации
        return str;
    }
}