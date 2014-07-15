package org.peerbox;

import org.peerbox.controller.H2HManager;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterValidation {

	public static void checkUsername(TextField username){
		//TODO
		
		if(H2HManager.INSTANCE.checkIfRegistered(username.getText())){
			System.out.println("This user is already registered");
		} else {
			System.out.println("This user is not registered so far.");
		}
	}
	
	public static void checkPassword(PasswordField pwd_1, PasswordField pwd_2){
		if (pwd_1.getLength() >= 6){
			System.out.println("Password length okay");
			
				if(pwd_1.getText().equals(pwd_2.getText())){
					System.out.println("Passwords identical.");
			} else {
				System.err.println("Passwords not identical. Try again");
			}
		} else {
			System.err.println("Password too short. Needs at least 6 characters!");
		}
	}
	
	public static void checkPIN(PasswordField pin_1, PasswordField pin_2){
		if (pin_1.getLength() >= 3){
			System.out.println("PIN length okay");
			
				if(pin_1.getText().equals(pin_2.getText())){
					System.out.println("PINs identical.");
			} else {
				System.err.println("PINs not identical. Try again");
			}
		} else {
			System.err.println("PIN too short. Needs at least 3 characters!");
		}
	}
}
