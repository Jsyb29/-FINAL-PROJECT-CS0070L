
import java.util.HashMap;
import java.util.Scanner;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class SportComplex {
    public static void main(String[] args) {

        //Name of the GUI
        JFrame frame = new JFrame("Sport Complex");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400,300);

        JTabbedPane tabbedPane = new JTabbedPane();

        //Creating the panels for the tabs
        JPanel Registration = new JPanel();
        

        JPanel Booking = new JPanel();
        Booking.add(new JTextField("Welcome to Booking Panel"));

        JPanel Database =  new JPanel();
        Database.add(new JTextField("Database: "));

//Adding the tabs into GUI
        tabbedPane.addTab("Registration", Registration);
                tabbedPane.addTab("Booking", Booking);
        tabbedPane.addTab("Database", Database);

        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.setVisible(true);


        //For customer tab
    
    JTextField CustoId = new JTextField(20);
    Registration.add(new JLabel("\nCustomer ID: "));
    Registration.add(CustoId);

    JTextField Name = new JTextField(20);
    Registration.add(new JLabel("Customer Name:"));
    Registration.add(Name);

    JTextField CustoType = new JTextField(20);
    Registration.add(new JLabel("Customer Type (Student, VIP, Regular): "));
    Registration.add(CustoType);

    JTextField PhoneNum = new JTextField(20);
    Registration.add(new JLabel("Customer Phone Number: "));
    Registration.add(PhoneNum);


    // Searching duplicates in the 

    HashMap <String, Customer> customerMap = new HashMap<>();
    
    }
}