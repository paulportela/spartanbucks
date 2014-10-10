package spartaGoldPrototype;

import java.io.*;

public class Users {

	
	public void saveStatus(Serializable object){
		   try {
		      FileOutputStream saveFile = new FileOutputStream("usersHT.dat");
		      ObjectOutputStream out = new ObjectOutputStream(saveFile);
		      out.writeObject(object);
		      out.close();
		      saveFile.close();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		}
	
	public Object loadStatus() throws ClassNotFoundException{
		   Object result = null;
		   try {
		      FileInputStream saveFile = new FileInputStream("usersHT.dat");
		      ObjectInputStream in = new ObjectInputStream(saveFile);
		      result = in.readObject();
		      in.close();
		      saveFile.close();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }
		    return result;
		}
}
