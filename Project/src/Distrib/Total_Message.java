package Distrib;

public class Total_Message extends Message{


    private int seq;
    private int User_Id;
    private boolean deliverble;

    Total_Message(String message, Integer Clock, Integer Port, String UserName, String GroupName, int seq, int User_id, boolean deliverable){
        super(message, Clock, Port, UserName, GroupName);
        this.seq = seq;
        this.deliverble = deliverable;
        this.User_Id = User_id;
    }

    boolean GetDeliverable(){
        return deliverble;
    }

    void SetDeliverable(boolean deliverble){
        this.deliverble = deliverble;
    }

    int Get_Id(){
        return User_Id;
    }

    void Set_Id(int seq){
        this.User_Id = User_Id;
    }

    int Get_seq(){
        return seq;
    }

    void Set_seq(int seq){
        this.seq = seq;
    }

}
