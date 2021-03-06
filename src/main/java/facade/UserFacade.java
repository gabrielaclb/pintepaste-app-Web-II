package facade;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import models.ResponseModel;
import models.SessionModel;
import models.UserModel;
import util.DBAccess;
import util.Encrypter;
import util.JacksonMapper;
import util.ModelCache;
import util.PropertiesReader;
import util.Validator;

public class UserFacade {
	
	private DBAccess db;
	private PropertiesReader pReader;
	private ModelCache modelCache;
	private Validator validator;
	private JacksonMapper jackson;
	
	public UserFacade() {
		db = null;
		pReader = PropertiesReader.getInstance();
		modelCache = ModelCache.getInstance();
	}
	
	private boolean isValidated(String name, String password, String email) {
		pReader = PropertiesReader.getInstance();
		validator = new Validator();
		
		if(validator.whitespaceValidated(name, password, email) && validator.emailContainsDomans(pReader.getValue("ER"), email)
				&& !validator.hasSpecialCharacter(pReader.getValue("UR"), name)
				&& !validator.hasSpecialCharacter(pReader.getValue("PR"), password)
				&& validator.lengthValidated(name, password, 20)) {
			return true;
		}
		return false;
	}
	
	public String insertUser(HttpServletRequest request) throws SQLException, CloneNotSupportedException, JsonProcessingException {
		db = DBAccess.getConnection(pReader);
		ResultSet rs = null;
		ResponseModel<SessionModel> res = modelCache.getModel("Response");
		String salt = Encrypter.getSalt(10);
		
		try {
			UserModel user = jackson.jsonToPojo(request, UserModel.class);
			if(isValidated(user.getName(), user.getPassword(), user.getEmail())) {
				rs = db.execute(pReader.getValue("SELECT_USER"), user.getName(), user.getEmail());
				
				if(!rs.next()) {
					db.update(pReader.getValue("INSERT_USER"), user.getName(), Encrypter.getSecurePassword(user.getPassword() + salt), user.getEmail(), salt);
					res.setStatus(200);
				} else {
					res.setStatus(500);
				}
				rs.close();
				db.close();
			} else {
				res.setStatus(403);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return jackson.pojoToJson(res);
	}
	
}
