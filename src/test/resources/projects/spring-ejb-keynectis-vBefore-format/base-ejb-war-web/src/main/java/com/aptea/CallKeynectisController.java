package com.aptea;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * @author gregory.anne
 */
@Controller
@SessionAttributes
public class CallKeynectisController extends AbstractController {

    @EJB(mappedName = "com.aptea.ICallKeynectisServiceRemote")
    private ICallKeynectisServiceRemote callKeynectis;

    private ICallKeynectisServiceRemote sigisizeBeans() {
	try {
	    return (ICallKeynectisServiceRemote) new InitialContext().lookup("com.aptea.ICallKeynectisServiceRemote");
	} catch (NamingException ex) {
	    return null;
	}
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest hsr, HttpServletResponse hsr1) {
	if (callKeynectis == null){
	    callKeynectis = sigisizeBeans();
	}
	try {
	    callKeynectis.digitalSignature();
	    return new ModelAndView("index", "command", new CallKeynectisForm("success"));
	} catch(Exception exc){
	    return new ModelAndView("index", "command", new CallKeynectisForm(exc.getMessage()));
	}
    }
}
