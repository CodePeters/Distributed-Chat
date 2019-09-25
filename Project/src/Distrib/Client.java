package Distrib;

import java.util.Iterator;
import java.io.*;
import java.net.InetAddress;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

public class Client {

    private static int Port, User_Id;
    private static int num, max_seq = 0;
    private static String UserName, My_ip;
    private static String chosen_group;   //the group that chooses to chat
    private static int group_counts = 0;   //In how many groups is the Client enrolled
    private static final int PACKETSIZE = 1024; //Length of message that can be send
    private static ConcurrentHashMap< String, Group > Groups = new  ConcurrentHashMap<>(998);  //the Groups that the Client is enrolled
    private static ConcurrentHashMap< String,ConcurrentHashMap<Integer, Integer> > Group_Vectors = new ConcurrentHashMap<>(998); //PORT TO VECTORS
    private static Group checkGroup;
    private static volatile int check ;
    private static ArrayList<Message> MessageQueue= new ArrayList<>(97);
    private static ConcurrentHashMap< String, Integer > ProposalNumber = new ConcurrentHashMap<>(998); //Group -> Proposal number for Total ordering
    private static Comparator<Total_Message> comp = new CustomComparator();
    private static final PriorityBlockingQueue<Total_Message> Total_queue = new PriorityBlockingQueue<>(97, comp);
    private static ConcurrentHashMap< String, Integer > User_clock = new ConcurrentHashMap<>(998);


    public static void main(String[] args) {


        BufferedReader in;
        PrintWriter out;
        BufferedReader stdIn;

        if( args.length != 2 )
        {
            System.out.println( "usage: DatagramServer port Mode[Total, Fifo]" ) ;
            return ;
        }else if(!args[1].equals("Fifo") &&  !args[1].equals("Total")){
            System.out.println( "second argument should be Fifo or Total" ) ;
            return ;
        }
        Port = Integer.parseInt( args[0] );

        stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;

        try{

            DatagramSocket UdpServerSocket = new DatagramSocket(Port);


            System.out.print("*************** - Client  Application - ***************\r\n\n");
            System.out.print("## Please Insert a Valid User Name ##\r\n");

            UserName = stdIn.readLine();

            System.out.print("## Registering User ##\r\n");

            //******************************** SET UDP HANDLER *********************************

            UDP_Connection_Handler handler = new UDP_Connection_Handler(UdpServerSocket, args);
            handler.start();

            //******************************** END UDP HANDLER *********************************


            //******************************** User Registration *******************************
            String Server_ip = InetAddress.getLocalHost().getHostAddress();
            My_ip = Server_ip;
            System.out.println(""+My_ip + " " + UdpServerSocket.getLocalAddress().getHostAddress());

            try {

                Socket socket = new Socket(Server_ip, 9002);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.print("!reg "+ My_ip +" "+Port+" "+UserName+"\r\n");
                out.flush();

                String response = in.readLine();
                if(response == null) {System.out.println("Cant Get an Id -> Exiting... !!");}
                else User_Id = Integer.parseInt(response);
                System.out.print(""+User_Id+"\n");

                out.close();
                in.close();
                socket.close();


            } catch (  IOException e ) {
                e.printStackTrace();
            }

            System.out.print("## User Registered !! ##\n");
            System.out.print("[" + UserName + "] > " );

            //******************************* User Registered !! *******************************


            //******************************** Heartbeat Messages ******************************
            Timer timer = new Timer();

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    BufferedReader in;
                    PrintWriter out;

                    try {

                        Socket socket = new Socket(Server_ip, 9002);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        out = new PrintWriter(socket.getOutputStream(), true);

                        out.print("!Heartbeat "+User_Id+"\r\n");

                        out.close();
                        in.close();
                        socket.close();

                    } catch (  IOException e ) {
                        e.printStackTrace();
                    }
                }
            }, 6000, 6000);
            //**********************************************************************************


            //********************************* USER INTERFACE *********************************

            while ((userInput = stdIn.readLine())!= null) {

                if(userInput.equals("!lg")) lg(Server_ip);
                else if(userInput.matches("!lm [\\w@*]*")) lm(userInput.substring(4), Server_ip);
                else if(userInput.matches("!j [\\w@*]*")){
                    String GroupName = userInput.substring(3);
                    if(Groups.containsKey(GroupName)){
                        System.out.print("Warning: You have already joined this group !!!\n");
                        System.out.print("[" + UserName + "] > ");
                        continue;
                    }
                    j(GroupName, Server_ip);
                    if(group_counts == 0) chosen_group = GroupName;
                    group_counts++;
                    check = 0;
                    multicast_to_others(GroupName,"!j " + GroupName + " " + User_Id + " " + UserName + " " + Port + " " + My_ip, UdpServerSocket);

                    try {
                        checkGroup = Groups.get(GroupName).Clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }

                    num = checkGroup.get_Users().size() - 1;
                    while (check < num) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    ProposalNumber.put(GroupName, 0);
                    User_clock.put(GroupName, 0);

                }
                else if(userInput.matches("!w [\\w@*]*")) {
                    if(Groups.contains(userInput.substring(3))) 
                        chosen_group = userInput.substring(3);
                    else {
                        System.out.println("\rYou have not joined this group");
                        System.out.print("[" + UserName + "] > ");
                    }
                }
                else if(userInput.matches("!e [\\w@*]*")) {
                    String GroupName = userInput.substring(3);
                    e(GroupName, Server_ip);

                    multicast_to_others(GroupName,"!e " + GroupName + " " + User_Id + " " + UserName + " " + Port + " " + My_ip , UdpServerSocket);

                    Groups.remove(GroupName);
                    Group_Vectors.remove(GroupName);
                    ProposalNumber.remove(GroupName);
                    User_clock.remove(GroupName);

                    if(GroupName.equals(chosen_group) || group_counts == 0) {
                        chosen_group = null;
                        System.out.print("Warning: No Chosen Group to chat!!!\n");
                    }
                    group_counts--;
                }
                else if(userInput.equals("!q")) {
                    quit( Server_ip );
                    System.out.println("[" + UserName + "] > Quiting ...");
                    //not necessary
                    Set<String> keyset = Groups.keySet();

                    for(String key: keyset) {
                        multicast_to_others(key , "!e " + key + " " + User_Id + " " + UserName + " " + Port + " " + My_ip, UdpServerSocket);
                    }

                    handler.interrupt();
                    stdIn.close();
                    UdpServerSocket.close();
                    System.exit(0);
                }
                else{
                    //message to send
                    if(chosen_group == null && group_counts == 0) {
                        System.out.print("Warning: Please join a group to send message !!!\n");
                        System.out.print("[" + UserName + "] > ");
                        continue;
                    }
                    else if(chosen_group == null){
                        System.out.print("Warning: No Chosen Group to chat!!!\n");
                        System.out.print("[" + UserName + "] > ");
                        continue;
                    }
                    int clock = User_clock.get(chosen_group) + 1;   //++;
                    User_clock.replace(chosen_group, clock);

                    if(args[1].equals("Fifo"))
                        multicast_to_all(chosen_group, "" + clock + " " + UserName + " " + chosen_group + " " + userInput, UdpServerSocket);
                    else{

                        try {
                            checkGroup = Groups.get(chosen_group).Clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }

                        max_seq = 0;
                        check = 0;
                        multicast_to_all(chosen_group, "!seq " + clock + " " + User_Id + " " + UserName + " " + chosen_group + " " + My_ip + " " + userInput, UdpServerSocket);

                        num = checkGroup.get_Users().size() - 1;
                        while (check < num) {
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        multicast_to_all(chosen_group, "" + clock + " " + User_Id + " " + max_seq + " " + chosen_group, UdpServerSocket);
                        int  Ag = ProposalNumber.get(chosen_group);
                        ProposalNumber.replace(chosen_group, Math.max(Ag, max_seq) + 1);
                    }


                }

                System.out.print("\r[" + UserName + "] > ");

            }
            //******************************** USER INTERFACE END*******************************

        } catch(IOException e){
            e.printStackTrace();
        }

    }



    private static class UDP_Connection_Handler extends Thread {

        private DatagramSocket UdpServerSocket;
        private String mode;

        UDP_Connection_Handler(DatagramSocket UdpServerSocket, String [] args){
            this.UdpServerSocket = UdpServerSocket;
            mode = args[1];
        }

        public void  run() {

            try{
                /*
                     NOTICE!! - THREAD SAFETY IS GRANTED - UdpServerSocket can be
                     used from main and User_Interface at the same time without problem !!
                */

                //noinspection InfiniteLoopStatement
                while (true) {

                    DatagramPacket packet = new DatagramPacket( new byte[PACKETSIZE], PACKETSIZE ) ;

                    // Receive a packet (blocking)
                    UdpServerSocket.receive( packet ) ;
                    int offset = packet.getOffset();
                    String response = new String(packet.getData(), Charset.forName("UTF-8")).substring(offset,offset+packet.getLength());
                    String contents[] = response.split("\\s+");

                    if(response.startsWith("!j")){

                        //message of the form: !j Groupname  User_Id  UserName Port Server_ip
                        if(Groups.containsKey(contents[1])) {
                            int clock = User_clock.get(contents[1]);
                            Groups.get(contents[1]).set_new_member(new four<>(Integer.parseInt(contents[2]), contents[3], Integer.parseInt(contents[4]), contents[5]));
                            Group_Vectors.get(contents[1]).put(Integer.parseInt(contents[4]), 0); //set new clock in vector for new member
                            String message = "!Clock " + clock + " " + contents[1] + " " + Port;
                            DatagramPacket Sendpacket = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(contents[5]) , Integer.parseInt(contents[4]));
                            UdpServerSocket.send( Sendpacket ) ;
                        }
                    }
                    else if(response.startsWith("!e")){

                        //message of the form: !e Groupname  User_Id  UserName Port Server_ip
                        if(Groups.containsKey(contents[1])) {
                            Group Group_to_leave = Groups.get(contents[1]);

                            Iterator<four<Integer, String, Integer, String>> iter = Group_to_leave.get_Users().iterator();
                            while (iter.hasNext()) {
                                four<Integer, String, Integer, String> element = iter.next();
                                if (element.getFirst() == Integer.parseInt(contents[2]) && element.getSecond().equals(contents[3])
                                        && element.getThird() == Integer.parseInt(contents[4]) && element.getFourth().equals(contents[5])){
                                    //for the waiting problem when someone leaves before sending his clock or seq number
                                    if(checkGroup.Contains(element.getFirst(), element.getSecond(), element.getThird(), element.getFourth())) num--;
                                    iter.remove();
                                    break;
                                }
                            }

                            Group_Vectors.get(contents[1]).remove(Integer.parseInt(contents[4]));
                        }

                    }
                    else if(response.startsWith("!Clock")){
                        //no need to see if groupname exist due to check!!!
                        int port = Integer.parseInt(contents[3]);
                        int clock = Integer.parseInt(contents[1]);
                        Group_Vectors.get(contents[2]).replace(port, clock);
                        check++;
                    }
                    else if(response.startsWith("!seq")){
                        int proposed_seq = ProposalNumber.get(contents[4]) + 1 ;
                        String message = "!proposed " + proposed_seq ;
                        ProposalNumber.replace(contents[4],  proposed_seq);
                        DatagramPacket Sendpacket = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(contents[5]) , packet.getPort());
                        UdpServerSocket.send( Sendpacket ) ;
                        int Id = Integer.parseInt(contents[2]);
                        int clock = Integer.parseInt(contents[1]);
                        String user_name = contents[3];
                        String groupname = contents[4];
                        int port = packet.getPort();
                        Total_queue.add(new Total_Message(response.split(" ", 7)[6], clock , port, user_name, groupname, proposed_seq, Id, false));

                    }
                    else if(response.startsWith("!proposed")){
                        max_seq = Math.max(max_seq, Integer.parseInt(contents[1]));
                        check++;
                    }
                    else{
                        if(mode.equals("Fifo"))
                            FiFoOrder(new Message(response.split(" ", 4)[3] , Integer.parseInt(contents[0]), packet.getPort(), contents[1], contents[2]));
                        else{
                            String groupname = contents[3];
                            int seq = Integer.parseInt(contents[2]);
                            int clock = Integer.parseInt(contents[0]);
                            int userId = Integer.parseInt(contents[1]);
                            if(ProposalNumber.get(groupname) < seq) ProposalNumber.replace(groupname, seq);
                            TotalOrder(seq, clock, userId);
                        }

                    }

                }

            }
            catch( Exception e ) {
                e.printStackTrace();
            }

        }
    }



    private static void lg(String Server_ip){

        try {

            Socket socket = new Socket(Server_ip, 9002);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.print("!list_groups\r\n");
            out.flush();


            String response = in.readLine();
            String[] Groups;

            out.close();
            in.close();
            socket.close();

            if(response == null) {
                System.out.print("  Hmm.. Can't List Groups, something happened !!!\r\n");

                return;
            }
            else Groups = response.split("\\s+");


            System.out.print("  Group names (in alphabetical order) are:\r\n");

            for(String group: Groups){
                System.out.print("  " + group + "\r\n");
            }


        } catch (  IOException e ) {
            e.printStackTrace();
        }
    }


    private static void lm(String GroupName, String Server_ip){

        try {

            Socket socket = new Socket(Server_ip, 9002);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.print("!list_members " + GroupName + "\r\n");
            out.flush();


            String response = in.readLine();
            String[] Members;

            if(response == null) {
                System.out.print("  Hmm.. Can't List Members, something happened !!!\r\n");
                return;
            }
            else Members = response.split("\\s+");

            out.close();
            in.close();
            socket.close();


            System.out.print("  Members in Group:" + GroupName + " are:\r\n");

            for(String member: Members){
                System.out.print(member + "\r\n");
            }


        } catch (  IOException e ) {
            e.printStackTrace();
        }
    }


    private static void j(String GroupName, String Server_ip){

        try {
            Socket socket = new Socket(Server_ip, 9002);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            out.print("!join_group " + GroupName + " " + User_Id + "\r\n");
            out.flush();


            String response = in.readLine();
            String[] Members;

            out.close();
            in.close();
            socket.close();

            if(response == null) {
                System.out.print("Hmm.. Can't Return User's of this group ,in order to store them, something happened !!!\r\n");
                return;
            }
            else {
                System.out.println("   " + response);
                Members = response.split("\\s+");
                int elements = Members.length;
                Group new_Group = new Group(new four<>(Integer.parseInt(Members[0]), Members[1], Integer.parseInt(Members[2]), Members[3]));

                ConcurrentHashMap<Integer, Integer> FiFoVector = new ConcurrentHashMap<>();
                FiFoVector.put(Integer.parseInt(Members[2]), 0);

                for(int i=4; i<=elements-4; i+=4) {
                    new_Group.set_new_member(new four<>(Integer.parseInt(Members[i]), Members[i + 1], Integer.parseInt(Members[i + 2]), Members[i + 3]));
                    FiFoVector.put(Integer.parseInt(Members[i + 2]),0 ); //Port -> Clock 0
                }
                Groups.put(GroupName, new_Group);

                Group_Vectors.put(GroupName, FiFoVector);
            }


        } catch (  IOException e ) {
            e.printStackTrace();
        }
    }


    private static void e(String GroupName, String Server_ip){

        try {

            Socket socket = new Socket(Server_ip, 9002);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.print("!exit_group " + GroupName + " " + User_Id + "\r\n");
            out.flush();

            out.close();
            socket.close();

        } catch (  IOException e ) {
            e.printStackTrace();
        }
    }


    private static void quit(String Server_ip){

        try {

            Socket socket = new Socket(Server_ip, 9002);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.print("!quit " + User_Id + "\r\n");
            out.flush();

            out.close();
            socket.close();

        } catch (  IOException e ) {
            e.printStackTrace();
        }
    }


    private static void multicast_to_others(String groupName, String message, DatagramSocket UdpServerSocket){
        try{

            for(four<Integer, String, Integer, String> user: Groups.get(groupName).get_Users())
                if (user.getFirst() != User_Id) {
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(user.getFourth()), user.getThird());
                    UdpServerSocket.send(packet);
                }

        }catch (IOException e){
            e.printStackTrace();
        }
    }


    private static void multicast_to_all(String groupName, String message, DatagramSocket UdpServerSocket){
        try{

            for(four<Integer, String, Integer, String> user: Groups.get(groupName).get_Users()) {
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, InetAddress.getByName(user.getFourth()), user.getThird());
                UdpServerSocket.send(packet);
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void  FiFoOrder(Message M){ //maybe not synchronized method we'll see...
        String Group = M.GetInfo().getFourth();
        int port = M.GetInfo().getSecond();
        int prev_clock = Group_Vectors.get(Group).get(port); //Fourth is GroupName

            if(M.GetInfo().getFirst() == prev_clock + 1 ){

                System.out.print("\r   in Group: " + M.GetInfo().getFourth() + " User " + M.GetInfo().getThird() + " send Message "
                            + M.GetMessage() + "\n");//" with clock " + M.GetInfo().getFirst()+ "\n");
                System.out.print("\r[" + UserName + "] > ");

                //Update Clock
                Group_Vectors.get(Group).replace(port, prev_clock + 1);
                checkFiFo();
            }
            else if(M.GetInfo().getFirst() > prev_clock + 1){
                System.out.println("\r   reverse order: " + M.GetMessage() + "\n");// " with clock " + M.GetInfo().getFirst());
                System.out.print("\r[" + UserName + "] > ");
                MessageQueue.add(M);
            }
            else{
                System.out.print("\r   Received smaller clock dumping message...\n");
                System.out.print("\r   Received message: " + M.GetMessage() + " with clocks: " + M.GetInfo().getFirst() + " "+ (prev_clock + 1)+ "\n");
                System.out.print("\r[" + UserName + "] > ");

            }

    }

    private static void checkFiFo() {

        Iterator<Message> iter = MessageQueue.iterator();
        boolean again = false;

        while (iter.hasNext()) {

            Message M = iter.next();
            String Group = M.GetInfo().getFourth();
            int port = M.GetInfo().getSecond();
            int prev_clock = Group_Vectors.get(Group).get(port); //Fourth is GroupName
            if(M.GetInfo().getFirst() == prev_clock + 1 ){

                System.out.print( "\r   in Group: " + M.GetInfo().getFourth() + " User " + M.GetInfo().getThird() + " send Message "
                                + M.GetMessage() + "\n");//" with clock " + M.GetInfo().getFirst()+ "\n");
                System.out.print("\r[" + UserName + "] > ");

                //Update Clock
                if(!again) again = true;
                Group_Vectors.get(Group).replace(port, prev_clock + 1);
                iter.remove();
            }
        }
        if(again) checkFiFo();
    }

    private static  void TotalOrder(int seq, int clock, int userId){

        Total_Message new_message = null;
        Iterator<Total_Message> iter = Total_queue.iterator();
        while(iter.hasNext()){

            Total_Message mes = iter.next();
            if( mes.Get_Id() == userId && mes.GetInfo().getFirst() == clock){
                new_message = mes;
                new_message.SetDeliverable(true);
                new_message.Set_seq(seq);
                iter.remove();
                break;
            }

        }
        if(new_message != null) Total_queue.add(new_message);
        check_Total_queue();
    }


    private static  void check_Total_queue(){

        Total_Message message = Total_queue.peek();
        if( message == null) return;
        boolean again = false;

        if(message.GetDeliverable()){


            Iterator<Total_Message> iter = Total_queue.iterator();
            while(iter.hasNext()){

                Total_Message M = iter.next();
                String Group = M.GetInfo().getFourth();
                int port = M.GetInfo().getSecond();
                int prev_clock = Group_Vectors.get(Group).get(port); //Fourth is GroupName

                if(M.Get_Id() == message.Get_Id() && M.GetInfo().getFirst() == prev_clock + 1 && M.GetDeliverable() ){

                    System.out.print("\r   in Group: " + M.GetInfo().getFourth() + " User " + M.GetInfo().getThird() + " send Message "
                                    + M.GetMessage() + "\n");// " with clock " + M.GetInfo().getFirst()+ "\n");
                    System.out.print("\r[" + UserName + "] > ");

                    //Update Clock
                    if(!again) again = true;
                    Group_Vectors.get(Group).replace(port, prev_clock + 1);
                    iter.remove();
                }

            }

            if(again) check_Total_queue();

        }
    }


}
