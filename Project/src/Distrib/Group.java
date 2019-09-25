package Distrib;

import java.util.ArrayList;

class Group implements Cloneable{

    private ArrayList< four<Integer, String, Integer, String> > members;

    Group(four<Integer, String, Integer, String> NewMember){
        members =  new ArrayList<>(97);
        set_new_member(NewMember);
    }

    void set_new_member(four<Integer, String, Integer, String> NewMember){

        members.add(NewMember);
    }

    void remove(four<Integer, String, Integer, String> Member){

        members.remove(Member);
    }

    String Get_Users_in_String(){
        String result = "";
        for(four<Integer, String, Integer, String> member: members)  result = result + " " + member.getFirst()
                + " " + member.getSecond() + " " + member.getThird()+ " " + member.getFourth();
        return  result.trim();
    }

    String Get_Only_MemberNames_in_String(){
        String result = "";
        for(four<Integer, String, Integer, String> member: members)  result = result + " " + member.getSecond() ;
        return  result.trim();
    }

    ArrayList< four<Integer, String, Integer, String> > get_Users(){
        return members;
    }

    boolean Contains (int id, String UserName, int Port, String ip){
        for(four<Integer, String, Integer, String> member: members)
            if(member.getFirst() == id && member.getSecond().equals(UserName) && member.getThird() == Port && member.getFourth().equals(ip)){
                return true;
            }
        return false;
    }

    Group Clone() throws CloneNotSupportedException{
        return (Group)super.clone();
    }

}
