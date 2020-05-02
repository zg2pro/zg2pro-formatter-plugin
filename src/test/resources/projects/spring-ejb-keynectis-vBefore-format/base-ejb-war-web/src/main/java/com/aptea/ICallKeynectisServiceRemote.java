package com.aptea;


import javax.ejb.Remote;

/**
 * User: gregory.anne
 */
@Remote
public interface ICallKeynectisServiceRemote  {
	
	String digitalSignature();
    
}
