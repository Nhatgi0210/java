package chatapp;
public class Protocol {
    public static final String USER = "USER";
    public static final String MSG  = "MSG";   // broadcast
    public static final String DM   = "DM";    // DM <target> <text...>
    public static final String FILE = "FILE";  // FILE <target> <filename> <size> + bytes
    public static final String BYE  = "BYE";
}
