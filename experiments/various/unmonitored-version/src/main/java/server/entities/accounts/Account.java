package main.java.server.entities.accounts;

import java.util.ArrayList;
import java.util.List;

import main.java.server.entities.instruments.VirtualCard;

public class Account {
	public boolean opened;
	public Integer account_number;
	public double balance;
	public double credit;
	public double limit;
	public Integer owner;
	protected List<VirtualCard> cards;

	public Account(Integer uid, Integer anumber, Double credit, Double limit) {
		account_number = anumber;
		balance = 0;
		opened = false;
		owner = uid;
		this.credit = credit;
		this.limit = limit;
		cards = new ArrayList<VirtualCard>();
	}

	public Account(Integer uid, Integer anumber, Double credit) {
		account_number = anumber;
		balance = 0;
		opened = false;
		owner = uid;
		this.credit = credit;
		this.limit = 0.0;
		cards = new ArrayList<VirtualCard>();
	}
	
	public Account(Integer uid, Integer anumber) {
		account_number = anumber;
		balance = 0;
		opened = false;
		owner = uid;
		this.credit = 0;
		cards = new ArrayList<VirtualCard>();
	}
	
	public List<VirtualCard> getCards(){
		return cards;
	}
	
	public VirtualCard getCard(int card_no){
		for(VirtualCard c : getCards()){
			if(c.id == card_no){
				return c;
			}
		}
		
		return null;
	}

	public boolean addCard(VirtualCard card){
		this.cards.add(card);
		return true;
	}
	
	public void removeCard(int no) {
		this.cards.remove(getCard(no));
	}

	public Integer getAccountNumber() 
	{ 
		return account_number; 
	}
	public double getBalance()
	{
		return balance;
	}
	public Integer getOwner()
	{
		return owner;
	}
	
	public void activateAccount()
	{
		opened = true;
	}
	public void closeAccount() 
	{
		opened = false;
	}

	public boolean withdraw(double amount) 
	{
		if(balance >= amount){
			balance -= amount;
			return true;
		}
		else if(balance + credit >= amount){
			balance = 0;
			credit -= amount - balance;
			return true;
		}
		else{
			return false;
		}
	}
	public boolean deposit(double amount) 
	{
		if(balance >= 0){
			balance += amount;
		}
		else if(amount >= Math.abs(balance)){
			credit += Math.abs(balance);
			balance += amount;
		}
		else if(amount < Math.abs(balance)){
			credit += amount;
			balance += amount;
		}
		
		return true;
	}
	
}
