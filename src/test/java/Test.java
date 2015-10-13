import config.AppConfig;
import model.FriendInfo;
import scala.Char;
import scala.concurrent.Future;
import service.RenrenHttpClient;
import utils.CharUtil;
import utils.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;

import static akka.dispatch.Futures.future;

/**
 * Created by amour on 15-10-10.
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(CharUtil.removeIllegalChars("胜男ஐ尐"));

        BitSet bs = new BitSet();
        bs.set(0);
        bs.set(3);
        bs.set(6);
        System.out.println(bs.toByteArray().toString());


        Future<Pair<FriendInfo, List<FriendInfo>>> f = future(new Callable<Pair<FriendInfo, List<FriendInfo>>>() {
            public Pair<FriendInfo, List<FriendInfo>> call() throws Exception {
                List<FriendInfo> commonFriends = new ArrayList<>();

                RenrenHttpClient httpClient = RenrenHttpClient.getInstance();

                return new Pair<>(null, commonFriends);
            }
        }, null);

        Future<Pair<FriendInfo, List<FriendInfo>>> f2 = future( () -> {
                List<FriendInfo> commonFriends = new ArrayList<>();
                RenrenHttpClient httpClient = RenrenHttpClient.getInstance();
                return new Pair<>(null, commonFriends);
        }, null);
    }
}
