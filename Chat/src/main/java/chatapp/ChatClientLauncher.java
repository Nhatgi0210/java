package chatapp;

import javax.swing.*; import java.awt.*;

public class ChatClientLauncher {
  public static void main(String[] args){
    SwingUtilities.invokeLater(()->{
      JTextField host=new JTextField("127.0.0.1");
      JTextField port=new JTextField("5000");
      JTextField user=new JTextField("user"+(System.currentTimeMillis()%1000));
      JPanel p=new JPanel(new GridLayout(0,1,6,6));
      p.add(new JLabel("Host:")); p.add(host);
      p.add(new JLabel("Port:")); p.add(port);
      p.add(new JLabel("Username:")); p.add(user);
      int r=JOptionPane.showConfirmDialog(null,p,"Đăng nhập Chat",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
      if(r==JOptionPane.OK_OPTION){
        try{
          String h=host.getText().trim(); int prt=Integer.parseInt(port.getText().trim());
          String u=user.getText().trim().isEmpty()? "user"+(System.currentTimeMillis()%1000) : user.getText().trim();
          ChatClientGUI gui=new ChatClientGUI(h,prt,u); gui.setVisible(true);
        } catch(Exception ex){
          ex.printStackTrace();
          JOptionPane.showMessageDialog(null,"Không thể kết nối: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);
        }
      }
    });
  }
}
