package db;
import db.mongodb.MongoDBConnection;
import db.mysql.MySQLConnection;
// return the object which is connection of MongDB or MySQL depends on the parameter 
// Factory Pattern 
public class DBConnectionFactory {
	// This should change based on the pipeline.
	private static final String DEFAULT_DB = "mysql";
	
	public static DBConnection getConnection(String db) {
		switch (db) {
		case "mysql":
			// return new MySQLConnection();
			return new MySQLConnection();
		case "mongodb":
			// return new MongoDBConnection();
			return new MongoDBConnection();
		default:
			throw new IllegalArgumentException("Invalid db:" + db);
		}

	}

	public static DBConnection getConnection() {
		return getConnection(DEFAULT_DB);
	}
}



