public class VIPCustomer extends Customer {

    public VIPCustomer(int Id, String Name, int PhoneNum) {
        super(Id, Name, PhoneNum);
        
    }

    @Override 
    String getType(){return "VIP";}
    
}
