package tn.esprit;

import tn.esprit.util.MyConnection;

public class Main {
    public static void main(String[] args) {
        MyConnection.getInstance().getCnx();
        System.out.println("Connexion EduKids initialisee avec succes.");
    }
}
