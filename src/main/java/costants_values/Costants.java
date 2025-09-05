package costants_values;

public final class Costants {
    private Costants() {
    }
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "Gardaland_Database";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "1234";
    public static final String DB_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
}
