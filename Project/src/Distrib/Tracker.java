package Distrib;

import java.net.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;


public class Tracker {

    private static final int PORT = 9002;
    private static final int UDP_PORT = 9003;
    private static volatile int id = 0;
    private static ConcurrentHashMap< String, Group > Groups = new ConcurrentHashMap<>(998);
    private static ConcurrentHashMap< Integer, Timer> Timers = new ConcurrentHashMap<>();
    private static final ArrayList< four<Integer, String, Integer, String> > Users = new ArrayList<>(998);

    public static void main(String[] args) throws Exception {

        ServerSocket listener = new ServerSocket(PORT);
        DatagramSocket udpServerSocket = new DatagramSocket(UDP_PORT);
        try {
            //noinspection InfiniteLoopStatement
            while (true) {
                System.out.println("listening for new connections");
                new Connection_Handler(listener.accept(), udpServerSocket).start();
            }
        }catch(IOException e){
            e.printStackTrace();
        } finally {
            try{
                listener.close();
                udpServerSocket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }


    }

    private static class Connection_Handler extends Thread {

        private String command;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private  DatagramSocket UdpServerSocket;


        Connection_Handler(Socket socket, DatagramSocket UdpServerSocket) {

            this.socket = socket;
            this.UdpServerSocket = UdpServerSocket;
        }

        public void  run(){
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);


                command = in.readLine();
                String[] contents;
                if(command == null) {System.out.println("Failed to read from socket, Client must have closed it !!");}


                else if(command.matches("!Heartbeat ([1-9]|[1-9]*)")) {

                    contents = command.split("\\s+");
                    int UserId= Integer.parseInt(contents[1]);
                    Timers.get(UserId).cancel();
                    Timers.remove(UserId);

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {

                                four<Integer, String, Integer, String> user;
                                synchronized (Users) {
                                    user = Get_User_from_Userid(UserId);
                                    System.out.println("Deleted User with id: " + UserId + " !!!");
                                    quit(user, true);
                                }
                                Timers.remove(UserId);

                                Set<String> keyset = Groups.keySet();

                                for(String GroupName: keyset) {
                                    assert user != null;
                                    if(Groups.get(GroupName).Contains(UserId, user.getSecond(), user.getThird(), user.getFourth()  ))
                                        multicast_to_clients(UserId ,GroupName, "!e " + GroupName + " " + UserId + " " + user.getSecond() + " " + user.getThird() + " " + user.getFourth(), UdpServerSocket);
                                }

                        }
                    },10000);
                    Timers.put(UserId, timer);

                }

                else if(command.startsWith("!reg")) { //To do better handling of reg!!!
                    contents = command.split("\\s+");
                    int UserId = register(contents[1],Integer.parseInt(contents[2]),contents[3]);
                    out.print(""+UserId+"\r\n");
                    out.flush();
                    Timer timer = new Timer();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {

                                four<Integer, String, Integer, String> user;
                                synchronized (Users) {
                                    user = Get_User_from_Userid(UserId);
                                    System.out.println("Deleted !!!");
                                    quit(user, true);
                                }
                                Timers.remove(UserId);

                                Set<String> keyset = Groups.keySet();

                                for(String GroupName: keyset) {
                                    assert user != null;
                                    //System.out.println( UserId + " " + user.getSecond() + " " + Us);
                                    if(Groups.get(GroupName).Contains(UserId, user.getSecond(), user.getThird(), user.getFourth()  ))
                                        multicast_to_clients(UserId ,GroupName, "!e " + GroupName + " " + UserId + " " + user.getSecond() + " " + user.getThird() + " " + user.getFourth(), UdpServerSocket);
                                }

                        }
                    }, 10000);
                    Timers.put(UserId, timer);
                }
                else if(command.equals("!list_groups")){
                    List<String> groupNames = list_groups();
                    String result = "";
                    for(String group: groupNames) result = result+" "+group;
                    out.print(result.trim()+"\r\n");
                    out.flush();
                }
                else if(command.matches("!list_members [\\w@*]*")){
                    contents = command.split("\\s+");
                    out.print(list_members(contents[1])+"\r\n");
                    out.flush();
                }
                else if(command.matches("!join_group [\\w@*]* ([1-9]|[1-9]*)")){
                    contents = command.split("\\s+");
                    synchronized (Users) {
                        String send = join_group(contents[1], Get_User_from_Userid(Integer.parseInt(contents[2])));
                        out.print(send + "\r\n");
                    }
                    out.flush();
                }
                else if(command.matches("!exit_group [\\w@*]* ([1-9]|[1-9]*)")){
                    contents = command.split("\\s+");
                    synchronized (Users) {// synchronized since  Get_User_from_Userid returns a reference to a User from array Users which is common for all threads
                        exit_group(contents[1], Get_User_from_Userid(Integer.parseInt(contents[2])));
                    }
                }
                else if(command.matches("!quit ([1-9]|[1-9]*)")){
                    contents = command.split("\\s+");
                    synchronized (Users) {// synchronized since  Get_User_from_Userid returns a reference to a User from array Users which is common for all threads
                        quit(Get_User_from_Userid(Integer.parseInt(contents[1])), false);
                    }
                }



            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        /* Function used to register the user */
        private  int register(String IP, int Port, String Username){

            synchronized(Users){
                id++;
                Users.add(new four<>(id, Username, Port, IP));
                return id;
            }
        }

        /* Function that retruns a List with all Group Names sorted in Alphabetical Order */
        private List<String> list_groups(){

            Set<String> keys;

            keys = Groups.keySet();

            List<String> list = new ArrayList<>(keys);
            Collections.sort(list);
            return list;

        }

        /* Function that returns the members of a group */
        private String list_members(String GroupName){
            synchronized(Users) {
                if(Groups.contains(GroupName))
                    return Groups.get(GroupName).Get_Only_MemberNames_in_String();
                else return "This group does not exist!!!";
            }
        }

        // A Function used for a client to join a group
        private String join_group(String GroupName, four<Integer, String, Integer, String> User){

                Group Group_to_return;

                if(Groups.containsKey(GroupName)){
                    Group_to_return = Groups.get(GroupName);
                    Group_to_return.set_new_member(User);

                }
                else {
                    Group_to_return = new Group(User);
                    Groups.put(GroupName, Group_to_return);
                }

                return Group_to_return.Get_Users_in_String();
        }

        // A Function used for a client to exit a group
        // return value 1 -> everything went OK, 0 -> User group not existed
        private int exit_group(String GroupName, four<Integer, String, Integer, String> User){

                if(Groups.containsKey(GroupName)){
                    Groups.get(GroupName).remove(User);
                    return 1;
                }
                else return 0;

        }

        //A function to get User information from Userid
        private four<Integer, String, Integer, String> Get_User_from_Userid(int Userid){

            synchronized (Users) {

                four<Integer, String, Integer, String> User_information;
                for (four<Integer, String, Integer, String> user : Users)
                    if (user.getFirst().equals(Userid)) {
                        User_information = user;
                        return User_information;
                    }
                return null;
            }

        }

        // A Function used for a client to quit
        private void quit(four<Integer, String, Integer, String> User, boolean Quited_From_Timer){

                if(!Quited_From_Timer){
                    Timers.get(User.getFirst()).cancel();
                    Timers.remove(User.getFirst());
                }

                List<String> GroupNames = list_groups();
                for (String group_name : GroupNames) {
                    exit_group(group_name, User);
                }
                synchronized (Users) {
                        Users.remove(User);
                }
        }

    }

    private static void multicast_to_clients(int User_Id, String groupName, String message, DatagramSocket UdpServerSocket){
        try{

            for(four<Integer, String, Integer, String> user: Groups.get(groupName).get_Users())
                if (user.getFirst() != User_Id) {
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(user.getFourth()), user.getThird());
                    UdpServerSocket.send(packet);
                }

            UdpServerSocket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }


}
