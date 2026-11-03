import java.util.HashSet;

public class Customer{
    private int Id;
    private String Name;
    private int PhoneNum;

    public Customer(int id2, String name2, int phoneNum2){
        this.Id= id2;
        this.Name=name2;
        this.PhoneNum=phoneNum2;
    }

    int getId(){return Id;}
    String getName(){return Name;}
    int getPhoneNum(){return PhoneNum;}
    String getType(){return "Regular";}

    
    }
