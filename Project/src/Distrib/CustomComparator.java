package Distrib;

import java.util.Comparator;

class CustomComparator implements Comparator<Total_Message> {

    @Override
    public int compare(Total_Message T1, Total_Message T2){

        if(T1.Get_seq() < T2.Get_seq()) return -1;
        else if(T1.Get_seq() > T2.Get_seq()) return 1;
        else if( T1.Get_Id() < T2.Get_Id()) return -1;
        else if( T1.Get_Id() > T2.Get_Id()) return 1;
        else return 0;
    }

}
