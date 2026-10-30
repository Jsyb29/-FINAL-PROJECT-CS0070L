public class StudentCustomer extends Customer{

    public StudentCustomer(int Id, String Name, int PhoneNum){
        super(Id, Name, PhoneNum);
    }

    @Override 
    String getType(){return "Student";}

}