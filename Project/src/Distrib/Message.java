package Distrib;


class Message {

    private final String message;
    private final four<Integer, Integer, String, String> info;

    Message(String message, Integer Clock, Integer Port, String UserName, String GroupName){
        this.message = message;
        info = new four<>(Clock, Port, UserName, GroupName);
    }

    String GetMessage(){
        return  message;
    }

    four<Integer, Integer, String, String> GetInfo(){
        return info;
    }


}
