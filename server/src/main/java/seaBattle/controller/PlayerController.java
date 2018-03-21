package seaBattle.controller;

import org.apache.log4j.Logger;
import seaBattle.model.Player;
import seaBattle.Server;
import seaBattle.model.Ship;
import seaBattle.xmlservice.InServerXML;
import seaBattle.xmlservice.OutServerXML;
import seaBattle.xmlservice.SaveLoadServerXML;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.Socket;

/**
 * <code>PlayerController</code> is the main communication unit that
 * handles thread with client, receives messages and provides answers.
 * Though <code>PlayerController</code> user do all actions related to Server part of game.
 * @author Oleksandr Symonenko
 */

public class PlayerController extends Thread {
    private Socket socket;
    private String str;
    private InServerXML inServerXML;
    private OutServerXML outServerXML;
    private Player thisPlayer;
    private GameController gc;
    private boolean waitingForReply;
    private final static Logger logger = Logger.getLogger(PlayerController.class);

    public PlayerController() {
        thisPlayer = new Player();
    }

    /**
     * <code>run</code> adds PlayerController thread ability to receive XML data
     * from linked Client and send appropriate XML data answers, based on keys/values in incoming XMLs
     */
    public void run() {
        thisPlayer.setStatus("offline");
        try {
            while (!socket.isClosed()){
                inServerXML.setReader(inServerXML.getFactory().createXMLStreamReader(inServerXML.getFileReader()));
                XMLStreamReader reader = inServerXML.getReader();
                while (reader.hasNext()) {
                    if (reader.getEventType() == 1 && "key".equals(reader.getLocalName())){
                        reader.next();
                        switch (inServerXML.checkValue(reader)){
                            case "LOG IN": {
                                String login = inServerXML.checkValue(reader);
                                thisPlayer.setLogin(login);
                                String password = inServerXML.checkValue(reader);
                                thisPlayer.setPassword(password);
                                outServerXML.send("LOG IN", authResult(login,password));
                                updateAndSendPlayersInfo();
                                break;
                            }
                            case "LOG OUT": {
                                String login = inServerXML.checkValue(reader);
                                outServerXML.send("LOG OUT",logoutResult(login));
                                updateAndSendPlayersInfo();
                                break;
                            }
                            case "REG": {
                                String login = inServerXML.checkValue(reader);
                                String password = inServerXML.checkValue(reader);
                                outServerXML.send("REG", regResult(login,password));
                                Server.updateAllPlayersSet();
                                break;
                            }
                            case "INVITE": {
                                waitingForReply = true;
                                String player1 = inServerXML.checkValue(reader);
                                String player2 = inServerXML.checkValue(reader);
                                inviteResult(player1,player2);
                                break;
                            }
                            case "REPLY": {
                                String player1 = inServerXML.checkValue(reader);
                                String reply = inServerXML.checkValue(reader);
                                str = replyResult(player1,reply);
                                if ("OK".equals(str)) {
                                    updateAndSendPlayersInfo();
                                }
                                break;
                            }
                            case "SHIP LOCATION": {
                                int x1 = Integer.parseInt(inServerXML.checkValue(reader));
                                int y1 = Integer.parseInt(inServerXML.checkValue(reader));
                                int x2 = Integer.parseInt(inServerXML.checkValue(reader));
                                int y2 = Integer.parseInt(inServerXML.checkValue(reader));
                                getOutServerXML().send("SHIP LOCATION", gc.setShip(this, new Ship(new int[]{x1, y1, x2, y2})));
                                if (gc.checkStartGame()) {
                                    gc.getPlayerController1().getOutServerXML().send("START GAME","READY");
                                    gc.getPlayerController2().getOutServerXML().send("START GAME","READY");
                                    gc.changeCurrentPlayer();
                                    gc.startTimer();
                                }
                                System.out.println();
                                break;
                            }
                            case "SHOOT": {
                                String x1 = inServerXML.checkValue(reader);
                                String y1 = inServerXML.checkValue(reader);
                                getOutServerXML().send("SHOOT RESULT",shootResult(this, x1,y1));
                                break;
                            }
                            case "SURRENDER": {
                                String player = inServerXML.checkValue(reader);
                                gc.surrender(this);
                                break;
                            }
                            case "MSG": {
                                String msg = inServerXML.checkValue(reader);
                                msgResult(msg);
                                break;
                            }
                            case "BAN PLAYER": {
                                String playerToBan = InServerXML.checkValue(reader);
                                String result = banPlayerResult(playerToBan);
                                if ((result).contains("kicked")) {
                                    updateAndSendPlayersInfo();
                                }
                                msgServer(result);
                                break;
                            }
                            case "BAN IP": {
                                String ipToBan = InServerXML.checkValue(reader);
                                String result = banIpResult(ipToBan);
                                if ((result).contains("kicked")) {
                                    updateAndSendPlayersInfo();
                                }
                                msgServer(result);
                                break;
                            }
                            case "UNBAN PLAYER": {
                                String playerToUnBan = InServerXML.checkValue(reader);
                                String result = unbanPlayerResult(playerToUnBan);
                                msgServer(result);
                                break;
                            }
                            case "UNBAN IP": {
                                String ipToUnBan = InServerXML.checkValue(reader);
                                String result = unbanIpResult(ipToUnBan);
                                msgServer(result);
                                break;
                            }
                            case "REBOOT": {
                                String admin = inServerXML.checkValue(reader);
                                msgServer("Server rebooting by " + admin);
                                rebootServer(admin);
                                break;
                            }
                            case "SHUTDOWN": {
                                String admin = inServerXML.checkValue(reader);
                                msgServer("Server is shutting down by " + admin);
                                shutdownServer(admin);
                                break;
                            }
                            default:{
                                str = "unknown key";
                                getOutServerXML().send("INFO", str);
                             }
                        }
                    }
                    if (reader.isEndElement() && "root".equals(reader.getName().toString())) {
                        break;
                    }else{
                        reader.next();
                    }
                }
                reader.close();
            }
            Server.getAllPlayersControllerSet().remove(this);
            logoutResult(thisPlayer.getLogin());
        } catch (XMLStreamException e) {
            logger.error("Receive/Send error between thread and client", e);
        } catch (IOException e) {
            logger.error("Kick player by ip error", e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error with clothing thread", e);
            }
        }
    }

    /**
     * <code>rebootServer</code> reboots server
     * @param login - needed for check access
     */
    private void rebootServer(String login) throws IOException {
        for (String login1 : Server.getAdminsSet()) {
            if (login1.equals(login)) {
                Server.reboot();
            }
        }
    }

    /**
     * <code>shutdownServer</code> is shutting server down
     * @param login - needed for check access
     */
    private void shutdownServer(String login) {
        for (String login1 : Server.getAdminsSet()) {
            if (login1.equals(login)) {
                Server.shutdown();
            }
        }
    }

    /**
     * <code>authResult</code> returns result of Client authorization
     * @param login - login
     * @param password - password
     * @return - result
     */
    public String authResult(String login, String password) {
        for (Player player : Server.getAllPlayersSet()){
            if (!player.getLogin().equals(login) && !player.getPassword().equals(password)){
                str = "player with this login or password not found. register first";
            }
            if (player.getLogin().equals(login)){
                if (!player.getPassword().equals(password)){
                    str = "password is incorrect";
                }
                if (player.getPassword().equals(password) && "online".equals(player.getStatus())){
                    str = "player with nickname \"" + login + "\" already logged in";
                    return str;
                }
                if (player.getPassword().equals(password)){
                    for (String admin : Server.getAdminsSet()){
                        if (player.getLogin().equals(admin)){
                            str = "success! admin access.";
                            return str;
                        }
                    }
                    if ("banned".equals(player.getStatus())){
                        str = "MORTAL, YOUR ACCOUNT IS BANNED BY HIGHER POWER!";
                        return str;
                    }
                    for (String ip : Server.getBannedIpListSet()){
                        if (socket.getInetAddress().toString().equals(ip)){
                            str = "MORTAL, YOUR IP IS BANNED BY HIGHER POWER!";
                            return str;
                        }
                    }
                    for (Player player1 : Server.getAllPlayersSet()){
                        if (player1.getLogin().equals(thisPlayer.getLogin())){
                            player1.setStatus("online");
                            thisPlayer.setStatus("online");
                            str = "success!";
                        }
                    }
                }
                return str;
            }
        }
        return str;
    }

    /**
     * <code>regResult</code> returns result of Client registration
     * @param login - login
     * @param password - password
     * @return - result
     */
    public String regResult(String login, String password){
        str = "";
        for (Player player : Server.getAllPlayersSet()){
            if (player.getLogin().equals(login)){
                str = "this login is already taken";
                break;
            }
        }
        if ("".equals(str)){
            Player player1 = new Player();
            player1.setLogin(login);
            player1.setPassword(password);
            player1.setStatus("offline");
            player1.setRank(100);

            Server.getAllPlayersSet().add(player1);
            str = "success!";
        }
        return str;
    }

    /**
     * <code>banPlayerResult</code> returns result of Client try to ban chosen player
     * @param playerToBan - players login to ban
     * @return - result
     */
    public String banPlayerResult(String playerToBan) {
        for (String login : Server.getAdminsSet()) {
            if (thisPlayer.getLogin().equals(login)) {
                for (Player pl1 : Server.getAllPlayersSet()) {
                    if (pl1.getLogin().equals(playerToBan)) {
                        if ("banned".equals(pl1.getStatus())){
                            str = "player " + playerToBan + " already banned";
                            return str;
                        }
                        pl1.setStatus("banned");
                        str = "player " + playerToBan + " was banned by " + thisPlayer.getLogin();
                        break;
                    }
                }
                try {
                    for (PlayerController pc1 : Server.getAllPlayersControllerSet()) {
                        if (pc1.getThisPlayer().getLogin().equals(playerToBan)) {
                            pc1.socket.close();
                            Server.getAllPlayersControllerSet().remove(pc1);
                            str = "player " + playerToBan + " was banned and kicked by " + thisPlayer.getLogin();
                            return str;
                        }
                    }
                } catch (IOException e) {
                    logger.error("Error with kicking player", e);
                }
            }
        }
        return str;
    }

    /**
     * <code>unbanPlayerResult</code> returns result of Client try to ban chosen player
     * @param playerToUnBan - players login to ban
     * @return - result
     */
    public String unbanPlayerResult(String playerToUnBan) {
        for (String login : Server.getAdminsSet()) {
            if (thisPlayer.getLogin().equals(login)) {
                for (Player pl1 : Server.getAllPlayersSet()) {
                    if (pl1.getLogin().equals(playerToUnBan)) {
                        if (!"banned".equals(pl1.getStatus())){
                            str = "player " + playerToUnBan + " already unbanned";
                            return str;
                        }
                        pl1.setStatus("offline");
                        str = "player " + playerToUnBan + " was unbanned by " + thisPlayer.getLogin();
                        return str;
                    }
                }
            }
        }
        str = "player with login " + playerToUnBan + " was not found in playerList.";
        return str;
    }

    /**
     * <code>banIpResult</code> returns result of Client try to ban chosen ip
     * @param ipToBan - ip to ban
     * @return - result
     * @throws IOException
     */
    public String banIpResult(String ipToBan) throws IOException {
        for (String login : Server.getAdminsSet()) {
            if (thisPlayer.getLogin().equals(login)) {
                for (String ip1 : Server.getBannedIpListSet()) {
                    if (ip1.equals(ipToBan)) {
                        str = "ip " + ipToBan + " already banned";
                        break;
                    }
                }
                Server.getBannedIpListSet().add(ipToBan);
                str = "ip " + ipToBan + " banned by " + thisPlayer.getLogin();
                for (PlayerController pc : Server.getAllPlayersControllerSet()) {
                    for (String ip : Server.getBannedIpListSet()) {
                        if (pc.socket.getInetAddress().toString().equals(ip)) {
                            str = "ip " + ipToBan + " banned, and " + pc.getThisPlayer().getLogin() + " kicked by " + thisPlayer.getLogin();
                            pc.socket.close();
                        }
                    }
                }
            }
        }
        return str;
    }

    /**
     * <code>unbanIpResult</code> returns result of Client try to ban chosen ip
     * @param ipToUnBan - ip to ban
     * @return - result
     * @throws IOException
     */
    public String unbanIpResult(String ipToUnBan) throws IOException {
        for (String login : Server.getAdminsSet()) {
            if (thisPlayer.getLogin().equals(login)) {
                for (String ip1 : Server.getBannedIpListSet()) {
                    if (ip1.equals(ipToUnBan)) {
                        Server.getBannedIpListSet().remove(ipToUnBan);
                        SaveLoadServerXML.updateBannedIpListXML();
                        str = "ip " + ipToUnBan + " was unbanned by " + thisPlayer.getLogin();
                        return str;
                    }
                }
                str = "ip " + ipToUnBan + " was not found in bannedIpList.";
            }
        }
        return str;
    }

    /**
     * <code>inviteResult</code> returns result of Client#1 invite player for play game
     * @param player1 - sender login
     * @param player2 - receiver login
     */
    private void inviteResult(String player1, String player2) {
        for (PlayerController pl: Server.getAllPlayersControllerSet()) {
            if (pl.getThisPlayer().getLogin().equals(player2)) {
                pl.getOutServerXML().send("INVITE",player1);
            }
        }
        outServerXML.send("INFO","invite send to player: " + player2);
    }

    /**
     * <code>inviteResult</code> returns result of Client#2 reply on invite for play game
     * @param player1 - receiver login
     * @param value - answer
     * @return
     */
    private String replyResult(String player1, String value) {

        for (PlayerController pc : Server.getAllPlayersControllerSet()) {
            if (pc.getThisPlayer().getLogin().equals(player1)) {
                if (pc.isWaitingForReply()) {
                    gc = new GameController(pc, this);
                    pc.setGc(gc);
                    for (Player player : Server.getAllPlayersSet()){
                        if (player.getLogin().equals(pc.getThisPlayer().getLogin())){
                            player.setStatus("ingame");
                        }
                        if (player.getLogin().equals(thisPlayer.getLogin())){
                            player.setStatus("ingame");
                        }
                    }
                    pc.getOutServerXML().send("START GAME", thisPlayer.getLogin());
                    outServerXML.send("START GAME", player1);
                    str = "OK";
                    break;
                }
                str = "ERROR! dude " + pc.getThisPlayer().getLogin() + " don't wanna play!";
            }
        }
        return str;
    }

    /**
     * <code>updateAndSendPlayersInfo</code> updates XML files and send their Sets copy
     * to all aviable Clients, that choosing by existence of their PlayerControllers
     */
    public void updateAndSendPlayersInfo(){
        Server.updateAllPlayersSet();
        Server.updateOnlinePlayersSet();
        Server.updateIngamePlayersSet();
        for (PlayerController pc : Server.getAllPlayersControllerSet()){
            if (!pc.socket.isClosed()){
                pc.sendOnlinePlayers();
                pc.sendIngamePlayers();
            }
        }
    }

    /**
     * <code>sendOnlinePlayers</code> uses by <code>updateAndSendPlayersInfo</code>
     */
    public void sendOnlinePlayers() {
        String[] list = new String[((Server.getOnlinePlayersSet().size())*2)+1];
        str = String.valueOf(Server.getOnlinePlayersSet().size());
        list[0] = str;
        int i = 1;
        for (Player player : Server.getOnlinePlayersSet()) {
            list[i++] = player.getLogin();
            list[i++] = String.valueOf(player.getRank());
        }
        outServerXML.send("ONLINE PLAYERS", list);
    }

    /**
     * <code>sendIngamePlayers</code> uses by <code>updateAndSendPlayersInfo</code>
     */
    public void sendIngamePlayers() {
        String[] list = new String[Server.getIngamePlayersSet().size()+1];
        str = String.valueOf(Server.getIngamePlayersSet().size());
        list[0] = str;
        int i = 1;
        for (Player player : Server.getIngamePlayersSet()) {
            list[i++] = player.getLogin();
        }
        outServerXML.send("INGAME PLAYERS", list);
    }

    /**
     * <code>msgServer</code> writes message to all Clients from server
     * @param msg - message
     */
    private void msgServer(String msg) {
        for (PlayerController pc : Server.getAllPlayersControllerSet()){
            pc.getOutServerXML().send("MSG", "SERVER: " + msg);
        }
    }

    /**
     * <code>msgResult</code> writes message to all Clients
     * @param msg - message
     */
    private void msgResult(String msg) {
        for (PlayerController pc : Server.getAllPlayersControllerSet()){
            pc.getOutServerXML().send("MSG", thisPlayer.getLogin() + ": " + msg);
        }
    }

    /**
     * <code>logoutResult</code> returns result of logging out Client
     * @param login - players login
     * @return - result
     */
    public String logoutResult(String login) {
        for (Player player : Server.getAllPlayersSet()){
            if (player.getLogin().equals(login) && "online".equals(player.getStatus())){
                player.setStatus("offline");
                str = "success!";
                Server.getAllPlayersControllerSet().remove(this);
                return str;
            }
        }
        str = "error, you already logged out";
        return str;
    }

    /**
     * <code>shootResult</code> returns result of shooting while playing game
     * @param playerController - Client's PlayerController who shoot
     * @param x1 - coordinate
     * @param y1 - coordinate
     * @return
     */
    private String shootResult(PlayerController playerController, String x1, String y1) {
        return gc.shoot(this,Integer.parseInt(x1),Integer.parseInt(y1));
    }

    public OutServerXML getOutServerXML() {
        return outServerXML;
    }

    public Player getThisPlayer() {
        return thisPlayer;
    }
    public void setThisPlayer(Player thisPlayer) {
        this.thisPlayer = thisPlayer;
    }

    public boolean isWaitingForReply() {
        return waitingForReply;
    }

    public GameController getGc() {
        return gc;
    }

    public void setGc(GameController gc) {
        this.gc = gc;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
        inServerXML = new InServerXML(socket);
        outServerXML = new OutServerXML(socket);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerController that = (PlayerController) o;

        if (waitingForReply != that.waitingForReply) return false;
        if (socket != null ? !socket.equals(that.socket) : that.socket != null) return false;
        return thisPlayer != null ? thisPlayer.equals(that.thisPlayer) : that.thisPlayer == null;
    }

    @Override
    public int hashCode() {
        int result = socket != null ? socket.hashCode() : 0;
        result = 31 * result + (thisPlayer != null ? thisPlayer.hashCode() : 0);
        result = 31 * result + (waitingForReply ? 1 : 0);
        return result;
    }
}
