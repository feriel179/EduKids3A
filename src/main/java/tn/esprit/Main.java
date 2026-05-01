package tn.esprit;

import com.edukids.utils.MyConnection;

public class Main {
    public static void main(String[] args) {
        MyConnection.getInstance().getCnx();
        System.out.println("Connexion EduKids initialisee avec succes.");
    }
}
