import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class SportComplex extends JFrame implements ActionListener {

    //Buttons
    JButton regisButton;
    JButton clearButton;

    //TextId
    static JTextField CustoId;
    static JTextField Name;
    JTextField CustoType;
    static JTextField PhoneNum;
    Map <String, Customer> customerMap = new HashMap<>();
    HashSet<String> phoneSet = new HashSet<>();

    public SportComplex(){

        super("Sport Complex");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        
JPanel Database1 = new JPanel(new BorderLayout());
JTable table = new JTable(databaseTableModel);
Database1.add(new JScrollPane(table), BorderLayout.CENTER);

        regisButton = new JButton("REGISTER");
        clearButton = new JButton("CLEAR");

        regisButton.addActionListener(this);
        clearButton.addActionListener(this);



        //Creating the panels for the tabs
        JPanel Registration = new JPanel();
        JPanel Booking = new JPanel();
        JPanel Database =  new JPanel();
        

//Adding the tabs into GUI

JTabbedPane tabbedPane = new JTabbedPane();
JPanel registration = new JPanel();
JPanel booking = new JPanel();
JPanel Database11 = new JPanel(new BorderLayout());

JTable table1 = new JTable(databaseTableModel);
Database11.add(new JScrollPane(table1), BorderLayout.CENTER);

        tabbedPane.addTab("Registration", Registration);
        tabbedPane.addTab("Booking", Booking);
        tabbedPane.addTab("Database", Database11);

        add(tabbedPane, BorderLayout.CENTER);

        


       
    //Add this text to the registration button
    CustoId = new JTextField(20);
    Registration.add(new JLabel("\nCustomer ID: "));
    Registration.add(CustoId);

     Name = new JTextField(20);
    Registration.add(new JLabel("Customer Name:"));
    Registration.add(Name);

     CustoType = new JTextField(20);
    Registration.add(new JLabel("Customer Type (Student, VIP, Regular): "));
    Registration.add(CustoType);

     PhoneNum = new JTextField(20);
    Registration.add(new JLabel("Customer Phone Number: "));
    Registration.add(PhoneNum);

    //add this button to the registration tab
    Registration.add(regisButton);
    Registration.add(clearButton);


    }
      
    public static void main(String[] args) {
SportComplex sportComplex = new SportComplex();
sportComplex.setVisible(true);
sportComplex.setSize(280, 350);
        


    }
        
 DefaultTableModel databaseTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Type", "Phone"}, 0);
 
    
// Where all buttons will work if you click it
    @Override
    public void actionPerformed(ActionEvent arg0) {
       String id = CustoId.getText();
       String name = Name.getText();
       String type = CustoType.getText();
       String phone = PhoneNum.getText();



  

if (arg0.getSource() == clearButton) {
    if (id.trim().isEmpty() && name.trim().isEmpty() && phone.trim().isEmpty() && type.trim().isEmpty()) {
        JOptionPane.showMessageDialog(rootPane, "The form is already empty", "Notice", JOptionPane.INFORMATION_MESSAGE);
    } else {
        CustoId.setText("");
        Name.setText("");
        CustoType.setText("");
        PhoneNum.setText("");
    }
    return;
}



// If you submit with lack of details it will cause an error message
if (id.trim().isEmpty() || name.trim().isEmpty() || phone.trim().isEmpty() || type.trim().isEmpty()) {
    JOptionPane.showMessageDialog(rootPane, "Please Complete the details", "Error", JOptionPane.ERROR_MESSAGE);
return;
}

int id1;
int phone1;
try {
    id1 = Integer.parseInt(id.trim());
    phone1 = Integer.parseInt(phone.trim());
} catch (NumberFormatException e) {
    JOptionPane.showMessageDialog(rootPane, "Customer ID and phone number must be numeric", "Error", JOptionPane.ERROR_MESSAGE);
    return;
}
String name1 = name.trim();
Customer newCustomer = new Customer(id1, name1, phone1);

 


   
// If the the type matches the type of the customer it will count as new customer.
// this feature also make organize the system, instead of centralized into customer.
// can use for the discount features in booking tab
if (type.equalsIgnoreCase("VIP")){
        newCustomer = new VIPCustomer(id1, name1, phone1);
    }
    else if (type.equalsIgnoreCase("Student")){
        newCustomer = new StudentCustomer(id1, name1, phone1);
    }
    else {
        newCustomer = new Customer(id1, name1, phone1);
    }


  
   if (customerMap.containsKey(String.valueOf(id1)) || phoneSet.contains(String.valueOf(phone1))) {
    JOptionPane.showMessageDialog(rootPane, "Customer ID already exists", "Error", JOptionPane.ERROR_MESSAGE);
// For the database storage.
// Condition: If the type is not vip, regular, or student it will not continue and will not store the info.
// but if the info is correct  even its capitalized or small caps it will store, proceed, and clear the form
    if (type.equalsIgnoreCase("VIP") || type.equalsIgnoreCase("student") || type.equalsIgnoreCase("Regular")) {
     // path of the file where the customer info will be stored   
    String file_path = "SportComplex_Registration.txt";

    // Move the success logic INSIDE the try block
    try (java.io.FileWriter writer = new java.io.FileWriter(file_path, true)) {
        writer.write("Customer ID: " + id1 + "\n" +
                     "Customer Name: " + name1 + "\n" +
                     "Customer Type: " + type + "\n" +
                     "Customer Phone Number: " + phone1 + "\n" +
                     "============================\n");
                     
// if the customer done registering it will show this message
        JOptionPane.showMessageDialog(rootPane, "Customer registered successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
// after submitting the form, it will clear the form for the next customer.
        CustoId.setText("");
        Name.setText("");
        CustoType.setText("");
        PhoneNum.setText("");
        customerMap.put(String.valueOf(id1), newCustomer);
    phoneSet.add(String.valueOf(phone1));

    }
    
    
    catch (java.io.IOException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(rootPane, "Failed to save data: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
    return;
    }

} 
else {
    // if the customer type doesnt meet the requirements, this message will pop up
    JOptionPane.showMessageDialog(rootPane, "Invalid customer type", "Error", JOptionPane.ERROR_MESSAGE);
}}}


//duplicate checker 








    }


  