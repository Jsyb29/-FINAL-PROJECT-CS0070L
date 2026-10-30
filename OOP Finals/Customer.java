public class Customer{
    private int Id;
    private String Name;
    private int PhoneNum;

    public Customer(int Id, String Name, int PhoneNum){
        this.Id= Id;
        this.Name=Name;
        this.PhoneNum=PhoneNum;
    }

    int getId(){return Id;}
    String getName(){return Name;}
    int getPhoneNum(){return PhoneNum;}
    String getType(){return "Regular";}
}