import service.RenrenHttpClient;

public class Main {
    public static void main(String[] args) throws Exception {
        RenrenHttpClient controller = RenrenHttpClient.create();
        controller.runFriendNetwork();
    }
}
