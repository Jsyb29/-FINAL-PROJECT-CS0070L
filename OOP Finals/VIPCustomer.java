public class VIPCustomer extends Customer {

    public VIPCustomer(int id1, String name1, int phone1) {
        super(id1, name1, phone1);
        
    }

    

    @Override 
    String getType(){return "VIP";}
    
}
