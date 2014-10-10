package spartaGoldPrototype;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

public class Chat {

    public static void main(String[] args) throws Exception {
        JChannel ch = new JChannel("tcp.xml");

        ch.setReceiver(new ReceiverAdapter() {
            public void viewAccepted(View new_view) {
                System.out.println("view: " + new_view);
            }

            public void receive(Message msg) {
                System.out.println("<< " + msg.getObject() + " [" + msg.getSrc() + "]");
            }
        });

        ch.connect("SpartanBucks");

        for(;;) {
            String line = Util.readStringFromStdin(": ");
            ch.send(null, line);
        }
    }

}